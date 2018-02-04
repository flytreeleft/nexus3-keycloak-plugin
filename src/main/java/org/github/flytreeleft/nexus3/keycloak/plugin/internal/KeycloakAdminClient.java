package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.shiro.util.StringUtils;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.ClientAuthenticator;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.Http;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.HttpMethod;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Client needs service-account with at least "view-clients, view-users" roles for "realm-management"
 * Enable "Service Accounts Enabled" and configure "Service Accounts Roles"
 * https://gist.github.com/thomasdarimont/c4e739c5a319cf78a4cff3b87173a84b#file-keycloakadminclientexample-java-L27
 */
public class KeycloakAdminClient {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*");

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final AdapterConfig config;
    private Http http;
    private KeycloakTokenManager tokenManager;

    public KeycloakAdminClient(AdapterConfig config) {
        this.config = config;
    }

    public KeycloakAdminClient(InputStream config) {
        try {
            this.config = JsonSerialization.readValue(config, AdapterConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not parse configuration.", e);
        }
    }

    public AccessTokenResponse obtainAccessToken(String username, String password) {
        URI uri = KeycloakUriBuilder.fromUri(this.config.getAuthServerUrl())
                                    .path(ServiceUrlConstants.TOKEN_PATH)
                                    .build(this.config.getRealm());
        HttpMethod<AccessTokenResponse> httpMethod = getHttp().post(uri);

        httpMethod = httpMethod.form()
                               .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                               .param("username", username)
                               .param("password", password);

        if (this.config.isPublicClient()) {
            httpMethod.param(OAuth2Constants.CLIENT_ID, this.config.getResource());
        } else {
            httpMethod.authorizationBasic(this.config.getResource(),
                                          this.config.getCredentials().get("secret").toString());
        }

        return httpMethod.response().json(AccessTokenResponse.class).execute();
    }

    public ClientRepresentation getRealmClient(String clientId) {
        HttpMethod<List<ClientRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/clients",
                                                                          this.config.getRealm());

        List<ClientRepresentation> clients = httpMethod.param("clientId", clientId)
                                                       .authentication()
                                                       .response()
                                                       .json(new TypeReference<List<ClientRepresentation>>() {})
                                                       .execute();

        return clients != null && !clients.isEmpty() ? clients.get(0) : null;
    }

    public UserRepresentation getUser(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        HttpMethod<List<UserRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/users",
                                                                        this.config.getRealm());

        // https://github.com/keycloak/keycloak/blob/master/services/src/main/java/org/keycloak/services/resources/admin/UsersResource.java#L177
        boolean isEmail = EMAIL_PATTERN.matcher(username).matches();
        if (isEmail) {
            httpMethod.param("email", username);
        } else {
            httpMethod.param("username", username);
        }
        List<UserRepresentation> users = httpMethod.authentication()
                                                   .response()
                                                   .json(new TypeReference<List<UserRepresentation>>() {})
                                                   .execute();

        if (users != null) {
            for (UserRepresentation user : users) {
                // Note: We need to avoid someone try to register email as username to fake others.
                boolean matched = isEmail ? username.equals(user.getEmail()) : username.equals(user.getUsername());
                if (matched) {
                    return user;
                }
            }
        }
        return null;
    }

    public List<UserRepresentation> getUsers() {
        HttpMethod<List<UserRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/users",
                                                                        this.config.getRealm());

        return httpMethod.authentication().response().json(new TypeReference<List<UserRepresentation>>() {}).execute();
    }

    public List<UserRepresentation> findUsers(String searchText) {
        HttpMethod<List<UserRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/users",
                                                                        this.config.getRealm());

        // https://github.com/keycloak/keycloak/blob/master/services/src/main/java/org/keycloak/services/resources/admin/UsersResource.java#L177
        return httpMethod.param("search", searchText)
                         .authentication()
                         .response()
                         .json(new TypeReference<List<UserRepresentation>>() {})
                         .execute();
    }

    public RoleRepresentation getRealmClientRole(String clientId, String roleName) {
        ClientRepresentation client = getRealmClient(clientId);
        HttpMethod<RoleRepresentation> httpMethod = getHttp().get("/admin/realms/%s/clients/%s/roles/%s",
                                                                  this.config.getRealm(),
                                                                  client.getId(),
                                                                  roleName);

        return httpMethod.authentication().response().json(RoleRepresentation.class).execute();
    }

    public List<RoleRepresentation> getRealmClientRoles(String clientId) {
        ClientRepresentation client = getRealmClient(clientId);
        HttpMethod<List<RoleRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/clients/%s/roles",
                                                                        this.config.getRealm(),
                                                                        client.getId());

        return httpMethod.authentication().response().json(new TypeReference<List<RoleRepresentation>>() {}).execute();
    }

    public List<RoleRepresentation> getRealmClientRolesOfUser(String clientId, String username) {
        UserRepresentation user = getUser(username);

        if (user != null) {
            ClientRepresentation client = getRealmClient(clientId);
            HttpMethod<List<RoleRepresentation>> httpMethod = getHttp().get(
                    "/admin/realms/%s/users/%s/role-mappings/clients/%s",
                    this.config.getRealm(),
                    user.getId(),
                    client.getId());

            return httpMethod.authentication()
                             .response()
                             .json(new TypeReference<List<RoleRepresentation>>() {})
                             .execute();
        }
        return null;
    }

    public AdapterConfig getConfig() {
        return this.config;
    }

    public synchronized Http getHttp() {
        if (this.http == null) {
            HttpClient httpClient = HttpClients.createDefault();
            ClientAuthenticator clientAuthenticator = (HttpMethod httpMethod) -> {
                String token = getTokenManager().getAccessTokenString();

                httpMethod.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.ACCESS_TOKEN);
                httpMethod.authorizationBearer(token);
            };

            this.http = new Http(this.config, httpClient, clientAuthenticator);
        }

        return this.http;
    }

    private KeycloakTokenManager getTokenManager() {
        if (this.tokenManager == null) {
            this.tokenManager = new KeycloakTokenManager(this.config, this.http);
        }

        return this.tokenManager;
    }
}
