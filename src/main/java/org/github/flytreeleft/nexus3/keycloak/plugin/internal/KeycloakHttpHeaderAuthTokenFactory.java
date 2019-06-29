package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.util.List;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.Lists;
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
}
