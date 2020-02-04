package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationToken;

/**
 * Authentication HTTP Header for Keycloak
 * <p/>
 * The header should be:
 * <pre>
 * X-Keycloak-Sec-Auth: &lt;username&gt;:&lt;auth token&gt;
 * </pre>
 * Or put username and auth token to the following headers:
 * <pre>
 * X-Auth-Username: &lt;username&gt;
 * X-Auth-Token: &lt;auth token&gt;
 * </pre>
 */
public class KeycloakHttpHeaderAuthToken extends HttpHeaderAuthenticationToken {
    // Headers used if Keycloak directly in front of Nexus
    public static final String HTTP_HEADER_NAME = "X-Keycloak-Sec-Auth";
    // Headers used if Keycloak gatekeeper in front of Nexus
    // See https://github.com/flytreeleft/nexus3-keycloak-plugin/pull/37
    public static final String HTTP_HEADER_USERNAME = "X-Auth-Username";
    public static final String HTTP_HEADER_AUTH_TOKEN = "X-Auth-Token";

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
