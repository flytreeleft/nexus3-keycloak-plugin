package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationToken;

/**
 * Authentication HTTP Header for Keycloak
 * <p/>
 * The header should be:
 * <pre>
 * X-Keycloak-Sec-Auth: &lt;username&gt;:&lt;access token&gt;
 * </pre>
 */
public class KeycloakHttpHeaderAuthToken extends HttpHeaderAuthenticationToken {
    public static final String HTTP_HEADER_NAME = "X-Keycloak-Sec-Auth";

    private String principal;
    private String credentials;

    public KeycloakHttpHeaderAuthToken(String headerName, String headerValue, String host) {
        super(headerName, headerValue, host);

        String[] splits = super.getPrincipal().split(":");
        /* convert username to lowercase to prevent creating multiple user session */
        this.principal = splits.length > 0 ? splits[0].toLowerCase() : null;
        this.credentials = splits.length > 1 ? splits[1] : "";
    }

    @Override
    public String getPrincipal() {
        return this.principal;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }
}
