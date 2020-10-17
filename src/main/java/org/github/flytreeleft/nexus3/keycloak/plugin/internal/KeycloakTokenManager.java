package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.Http;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.HttpMethod;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakTokenManager {
    private static final long DEFAULT_MIN_VALIDITY = 30; // seconds

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AccessTokenResponse currentToken;
    private long expirationTime; // seconds
    private long minTokenValidity = DEFAULT_MIN_VALIDITY; // seconds
    private final AdapterConfig config;
    private final Http http;
    private final String grantType;

    public KeycloakTokenManager(AdapterConfig config, Http http) {
        this.config = config;
        this.http = http;
        this.grantType = OAuth2Constants.CLIENT_CREDENTIALS;

        if (config.isPublicClient()) {
            throw new IllegalArgumentException("Can't use "
                                               + OAuth2Constants.GRANT_TYPE
                                               + "="
                                               + OAuth2Constants.CLIENT_CREDENTIALS
                                               + " with public client");
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
        HttpMethod<AccessTokenResponse> httpMethod = tokenRequest(OAuth2Constants.REFRESH_TOKEN);
        httpMethod.param(OAuth2Constants.REFRESH_TOKEN, currentToken.getRefreshToken());

        try {
            return updateToken(httpMethod);
        } catch (Exception e) {
            return grantToken();
        }
    }

    private AccessTokenResponse updateToken(HttpMethod<AccessTokenResponse> httpMethod) {
        int requestTime = Time.currentTime();

        synchronized (this) {
            this.currentToken = httpMethod.response().json(AccessTokenResponse.class).execute();
            this.expirationTime = requestTime + this.currentToken.getExpiresIn();

            this.logger.info("Token {} will be expired after {}s", this.currentToken, this.currentToken.getExpiresIn());
        }
        return this.currentToken;
    }

    private HttpMethod<AccessTokenResponse> tokenRequest(String grantType) {
        String path = "/realms/%s/protocol/openid-connect/token";
        HttpMethod<AccessTokenResponse> httpMethod = this.http.post(path, this.config.getRealm());
        httpMethod.param(OAuth2Constants.GRANT_TYPE, grantType);

        if (this.config.isPublicClient()) {
            httpMethod.param(OAuth2Constants.CLIENT_ID, this.config.getResource());
        } else {
            httpMethod.authorizationBasic(this.config.getResource(),
                                          this.config.getCredentials().get("secret").toString());
        }

        return httpMethod;
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
