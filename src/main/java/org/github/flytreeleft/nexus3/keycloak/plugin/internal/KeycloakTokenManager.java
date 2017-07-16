package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.Http;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.HttpMethod;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.adapters.config.AdapterConfig;

import static org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS;
import static org.keycloak.OAuth2Constants.CLIENT_ID;
import static org.keycloak.OAuth2Constants.GRANT_TYPE;
import static org.keycloak.OAuth2Constants.REFRESH_TOKEN;

public class KeycloakTokenManager {
    private static final long DEFAULT_MIN_VALIDITY = 30;

    private AccessTokenResponse currentToken;
    private long expirationTime;
    private long minTokenValidity = DEFAULT_MIN_VALIDITY;
    private final AdapterConfig config;
    private final Http http;
    private final String grantType;

    public KeycloakTokenManager(AdapterConfig config, Http http) {
        this.config = config;
        this.http = http;
        this.grantType = CLIENT_CREDENTIALS;

        if (config.isPublicClient()) {
            throw new IllegalArgumentException(
                    "Can't use " + GRANT_TYPE + "=" + CLIENT_CREDENTIALS + " with public client");
        }
    }

    public String getAccessTokenString() {
        return getAccessToken().getToken();
    }

    public synchronized AccessTokenResponse getAccessToken() {
        if (this.currentToken == null) {
            grantToken();
        } else if (tokenExpired()) {
            refreshToken();
        }
        return this.currentToken;
    }

    public AccessTokenResponse grantToken() {
        HttpMethod<AccessTokenResponse> httpMethod = tokenRequest(this.grantType);

        return updateToken(httpMethod);
    }

    public AccessTokenResponse refreshToken() {
        HttpMethod<AccessTokenResponse> httpMethod = tokenRequest(REFRESH_TOKEN);
        httpMethod.param(REFRESH_TOKEN, currentToken.getRefreshToken());

        try {
            return updateToken(httpMethod);
        } catch (Exception e) {
            return grantToken();
        }
    }

    private HttpMethod<AccessTokenResponse> tokenRequest(String grantType) {
        String path = "/realms/%s/protocol/openid-connect/token";
        HttpMethod<AccessTokenResponse> httpMethod = this.http.post(path, this.config.getRealm());
        httpMethod.param(GRANT_TYPE, grantType);

        if (this.config.isPublicClient()) {
            httpMethod.param(CLIENT_ID, this.config.getResource());
        } else {
            httpMethod.authorizationBasic(this.config.getResource(),
                                          this.config.getCredentials().get("secret").toString());
        }

        return httpMethod;
    }

    private AccessTokenResponse updateToken(HttpMethod<AccessTokenResponse> httpMethod) {
        int requestTime = Time.currentTime();

        synchronized (this) {
            this.currentToken = httpMethod.response().json(AccessTokenResponse.class).execute();
            this.expirationTime = requestTime + this.currentToken.getExpiresIn();
        }
        return this.currentToken;
    }

    public synchronized void setMinTokenValidity(long minTokenValidity) {
        this.minTokenValidity = minTokenValidity;
    }

    private synchronized boolean tokenExpired() {
        return (Time.currentTime() + this.minTokenValidity) >= this.expirationTime;
    }

    /**
     * Invalidates the current token, but only when it is equal to the token passed as an argument.
     *
     * @param token
     *         the token to invalidate (cannot be null).
     */
    public void invalidate(String token) {
        if (this.currentToken == null) {
            return; // There's nothing to invalidate.
        }
        if (token.equals(this.currentToken.getToken())) {
            // When used next, this cause a refresh attempt, that in turn will cause a grant attempt if refreshing fails.
            this.expirationTime = -1;
        }
    }
}
