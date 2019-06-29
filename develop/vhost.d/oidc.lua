local http = require "resty.http"
local cjson = require "cjson"

-- <<<<<<<< Source from https://github.com/zmartzone/lua-resty-openidc/blob/v1.5.3/lib/resty/openidc.lua
local function openidc_parse_json_response(response)
    local err
    local res
    -- check the response from the OP
    if response.status ~= 200 then
        err = "response indicates failure, status="..response.status..", body="..response.body
    else
        -- decode the response and extract the JSON object
        res = cjson.decode(response.body)
        if not res then
            err = "JSON decoding failed"
        end
    end
    return res, err
end

local function openidc_cache_get(type, key)
    local dict = ngx.shared[type]
    local value
    local flags
    if dict then
        value, flags = dict:get(key)
        if value then ngx.log(ngx.DEBUG, "cache hit: type=", type, " key=", key) end
    end
    return value
end

local function openidc_cache_set(type, key, value, exp)
    local dict = ngx.shared[type]
    if dict then
        local success, err, forcible = dict:set(key, value, exp)
        ngx.log(ngx.DEBUG, "cache set: success=", success, " err=", err, " forcible=", forcible)
    end
end

local function openidc_discover(url, ssl_verify)
    ngx.log(ngx.DEBUG, "In openidc_discover - URL is "..url)

    local json, err
    local v = openidc_cache_get("discovery", url)
    if not v then
        ngx.log(ngx.DEBUG, "Discovery data not in cache. Making call to discovery endpoint")
        -- make the call to the discovery endpoint
        local httpc = http.new()
        local res, error = httpc:request_uri(url, {
            ssl_verify = (ssl_verify ~= "no")
        })
        if not res then
            err = "accessing discovery url ("..url..") failed: "..error
            ngx.log(ngx.ERR, err)
        else
            ngx.log(ngx.DEBUG, "Response data: "..res.body)
            json, err = openidc_parse_json_response(res)
            if json then
                if string.sub(url, 1, string.len(json['issuer'])) == json['issuer'] then
                    openidc_cache_set("discovery", url, cjson.encode(json), 24 * 60 * 60)
                else
                    err = "issuer field in Discovery data does not match URL"
                    json = nil
                end
            else
                err = "could not decode JSON from Discovery data"
            end
        end
    else
        json = cjson.decode(v)
    end

    return json, err
end
-- >>>>>> End

local function openidc_send_error(err)
    ngx.status = 500
    ngx.say(err)
    ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
end

local function openidc_backend_logout(opts, session_opts)
    local discovery, err = openidc_discover(opts.discovery, opts.ssl_verify)
    if err then
        return openidc_send_error(err)
    end

    local session = require("resty.session").open(session_opts)
    local id_token = session.data.enc_id_token
    session:destroy()

    if opts.redirect_after_logout_uri then
        return ngx.redirect(opts.redirect_after_logout_uri)
    else
        local end_session_endpoint = discovery.end_session_endpoint or discovery.ping_end_session_endpoint
        local httpc = http.new()
        -- https://github.com/ledgetech/lua-resty-http#request
        local res, err = httpc:request_uri(end_session_endpoint, {
            method = "GET",
            query = {
                id_token_hint = id_token -- Pass encoded id_token
            },
            ssl_verify = (ssl_verify ~= "no")
        })
        if err then
            return openidc_send_error(err)
        end
    end

    ngx.header.content_type = "text/html"
    ngx.say("<html><body>Logged Out</body></html>")
    ngx.exit(ngx.OK)
end


local function oidc_check(opts, session_opts)
    -- https://github.com/pingidentity/lua-resty-openidc#sample-configuration-for-google-signin
    if ngx.var.oidc_ip_whitelist and ngx.var.remote_addr then
        for ip in string.gmatch(ngx.var.oidc_ip_whitelist, '([^, ]+)') do
            if ip == ngx.var.remote_addr then
                return
            end
        end
    end

    -- Change the redirect uri to the root uri to prevent to get 500 error
    local request_uri_args = ngx.req.get_uri_args()
    if ngx.var.request_uri == opts.redirect_uri_path and (not request_uri_args.code or not request_uri_args.state) then
        -- https://github.com/openresty/lua-nginx-module#ngxreqset_uri
        -- Note: 1. 'jump=true' isn't allowed in 'access_by_lua' directive
        --       2. 'ngx.req.set_uri' will not change the value of 'ngx.var.request_uri'
        --ngx.req.set_uri("/", false)
        return ngx.redirect("/")
    end

    -- Do logout in the background
    if ngx.var.request_uri == opts.logout_path and not opts.redirect_logout_url then
        return openidc_backend_logout(opts, session_opts)
    end

    -- Do authenticate
    local res, err = require("resty.openidc").authenticate(opts, nil, nil, session_opts)
    if err then
        ngx.status = 500
        ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
        return
    end

    -- for key, value in pairs(ngx.req.get_headers()) do
    --     local val = type(value) == 'string' and {value} or value
    --     for k, v in ipairs(val) do
    --         for i=0, v:len(), 1024 do
    --             ngx.log(ngx.DEBUG, 'Request Header: '..key..' -> '..v:sub(i + 1, i + 1024))
    --         end
    --     end
    -- end

    -- https://kubernetes.io/docs/admin/authentication/#authenticating-proxy
    if res.id_token.sub then
        -- Drop the old session to avoid to pass big cookie to the proxied backend
        local cookie = ngx.req.get_headers()['Cookie'];
        if string.match(cookie, "session_%d=") then
            -- Note: ngx.log() will only print the first 2048 bytes for the long log
            -- for i=0, cookie:len(), 1024 do
            --     ngx.log(ngx.DEBUG, "old cookies: "..cookie:sub(i + 1, i + 1024))
            -- end
            -- Lua Regex Pattern: https://riptutorial.com/lua/topic/5829/pattern-matching
            cookie = cookie:gsub('session=.-;', '')
            ngx.req.set_header('Cookie', cookie)
        end

        local username = res.id_token.username or res.id_token.preferred_username or (res.id_token.user and res.id_token.user.name)
        ngx.req.set_header("X-Remote-User", username)
        ngx.req.set_header("X-Remote-User-Access-Token", res.access_token)

        if res.id_token.groups then
            for i, group in ipairs(res.id_token.groups) do
                ngx.req.set_header("X-Remote-Group", group)
            end
        end
    else
        ngx.req.clear_header("X-Remote-USER")
        ngx.req.clear_header("X-Remote-GROUP")
        ngx.req.clear_header("X-Remote-User-Access-Token")
    end
end


local opts = {
    -- Redirect uri which doesn't exist and cannot be '/'
    redirect_uri_path = "/redirect_uri",
    discovery = ngx.var.oidc_discovery,
    client_id = ngx.var.oidc_client_id,
    client_secret = ngx.var.oidc_client_secret,
    ssl_verify = ngx.var.oidc_ssl_verify or "no",
    logout_path = ngx.var.oidc_logout_path,
    redirect_logout_url = not (ngx.var.oidc_redirect_logout_url == "false"),
    -- Prevent 'client_secret' to be nil:
    -- https://github.com/pingidentity/lua-resty-openidc/blob/v1.5.3/lib/resty/openidc.lua#L353
    token_endpoint_auth_method = "client_secret_post",
    --refresh_session_interval = 900,
    --access_token_expires_in = 3600,
    --force_reauthorize = false
}
-- Set a fixed and unique session secret for every domain to prevent infinite redirect loop
--   https://github.com/pingidentity/lua-resty-openidc/issues/32#issuecomment-273900768
--   https://github.com/openresty/lua-nginx-module#set_by_lua
local session_opts = {
    secret = ngx.encode_base64(ngx.var.server_name):sub(0, 32)
}

oidc_check(opts, session_opts)
