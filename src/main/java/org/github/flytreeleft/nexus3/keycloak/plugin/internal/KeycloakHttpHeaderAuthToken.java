package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationToken;

import java.util.Map;

/**
 * Authentication HTTP Header for Keycloak
 * <p/>
 * The header should be:
 * <pre>
 * X-Keycloak-Sec-Auth: &lt;username&gt;:&lt;access token&gt;
 * </pre>
 */
public class KeycloakHttpHeaderAuthToken extends HttpHeaderAuthenticationToken {
    //Headers used if Keycloak directly in front of Nexus
    public static final String HTTP_HEADER_NAME = "X-Keycloak-Sec-Auth";

    //Headers used if Keycloak gatekeeper in front of Nexus
    public static final String HTTP_HEADER_AUTH_TOKEN = "X-Auth-Token";
    public static final String HTTP_HEADER_USERNAME = "X-Auth-Username";

    private String principal;
    private String credentials;

    public KeycloakHttpHeaderAuthToken(String headerName, String headerValue, String host) {
        super(headerName, headerValue, host);

        this.updateCreedentials();
    }

    public KeycloakHttpHeaderAuthToken(Map<String, String> headers, String host) {
        super(HTTP_HEADER_NAME, headers.get(HTTP_HEADER_USERNAME) + ":" + headers.get(HTTP_HEADER_AUTH_TOKEN), host);
        this.updateCreedentials();
    }

    private void updateCreedentials(){
        String[] splits = super.getPrincipal().split(":");
        this.principal = splits.length > 0 ? splits[0] : null;
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
