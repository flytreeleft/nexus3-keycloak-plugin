package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.util.List;
import javax.annotation.Nullable;
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
    public AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        // Try to create auth token with the header X-Keycloak-Sec-Auth.
        AuthenticationToken token = super.createToken(request, response);

        // If the token is null, then try to create a new one with the header X-Auth-Token and X-Auth-Username
        if (token == null) {
            // See https://github.com/flytreeleft/nexus3-keycloak-plugin/pull/37
            HttpServletRequest httpRequest = WebUtils.toHttp(request);

            String username = httpRequest.getHeader(KeycloakHttpHeaderAuthToken.HTTP_HEADER_USERNAME);
            String authToken = httpRequest.getHeader(KeycloakHttpHeaderAuthToken.HTTP_HEADER_AUTH_TOKEN);

            if (username != null && authToken != null) {
                String headerValue = username + ":" + authToken;

                token = createToken(KeycloakHttpHeaderAuthToken.HTTP_HEADER_NAME, headerValue, request.getRemoteHost());
            }
        }

        return token;
    }

    @Override
    protected HttpHeaderAuthenticationToken createToken(String headerName, String headerValue, String host) {
        logger.debug("createToken with HTTP header: {name: {}, value: {}, host: {}}", headerName, headerValue, host);

        HttpHeaderAuthenticationToken token = new KeycloakHttpHeaderAuthToken(headerName, headerValue, host);

        return token.getPrincipal() != null ? token : null;
    }

    @Override
    protected List<String> getHttpHeaderNames() {
        return Lists.newArrayList(KeycloakHttpHeaderAuthToken.HTTP_HEADER_NAME);
    }
}
