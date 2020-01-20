package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.util.*;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationToken;
import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationTokenFactorySupport;

// https://github.com/flatwhite/nexus-2.x/blob/master/plugins/security/nexus-rutauth-plugin/src/main/java/org/sonatype/nexus/rutauth/internal/RutAuthAuthenticationTokenFactory.java
@Singleton
@Named
public class KeycloakHttpHeaderAuthTokenFactory extends HttpHeaderAuthenticationTokenFactorySupport {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakHttpHeaderAuthTokenFactory.class);

    @Override
    protected List<String> getHttpHeaderNames() {
        return Lists.newArrayList(KeycloakHttpHeaderAuthToken.HTTP_HEADER_NAME);
    }

    @Override
    protected HttpHeaderAuthenticationToken createToken(String headerName, String headerValue, String host) {
        logger.debug("createToken with HTTP header: {name: {}, value: {}, host: {}}", headerName, headerValue, host);

        HttpHeaderAuthenticationToken token = new KeycloakHttpHeaderAuthToken(headerName, headerValue, host);

        return token.getPrincipal() != null ? token : null;
    }

    @Override
    public AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);

        //First, check if X-Keycloak-Sec-Auth is present.
        List<String> headerNames = this.getHttpHeaderNames();
        if (headerNames != null) {
            Iterator var6 = headerNames.iterator();

            while(var6.hasNext()) {
                String headerName = (String)var6.next();
                String headerValue = httpRequest.getHeader(headerName);
                if (headerValue != null) {
                    // if X-Keycloak-Sec-Auth is present, use this header to generate the token
                    return this.createToken( headerName,  headerValue, request.getRemoteHost());
                }
            }
        }
        //If X-Keycloak-Sec-Auth not present, may be behind gatekeeper
        // So we use Authorization and X-Auth-Username headers to fill the X-Keycloak-Sec-Auth header
        String headerAuthTokenValue = httpRequest.getHeader(KeycloakHttpHeaderAuthToken.HTTP_HEADER_AUTH_TOKEN);
        String headerUsernameValue = httpRequest.getHeader(KeycloakHttpHeaderAuthToken.HTTP_HEADER_USERNAME);
        if (headerAuthTokenValue != null && headerUsernameValue != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put(KeycloakHttpHeaderAuthToken.HTTP_HEADER_AUTH_TOKEN, headerAuthTokenValue);
            headers.put(KeycloakHttpHeaderAuthToken.HTTP_HEADER_USERNAME, headerUsernameValue);
            HttpHeaderAuthenticationToken token = new KeycloakHttpHeaderAuthToken(headers, request.getRemoteHost());
            return token.getPrincipal() != null ? token : null;
        }

        return null;
    }
}
