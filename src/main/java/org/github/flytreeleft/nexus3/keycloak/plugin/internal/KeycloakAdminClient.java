package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.shiro.util.StringUtils;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.ClientAuthenticator;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.Http;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.HttpMethod;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client needs service-account with at least "view-clients, view-users" roles for "realm-management"
 * Enable "Service Accounts Enabled" and configure "Service Accounts Roles"
 * https://gist.github.com/thomasdarimont/c4e739c5a319cf78a4cff3b87173a84b#file-keycloakadminclientexample-java-L27
 * <p/>
 * API: https://www.keycloak.org/docs-api/3.4/rest-api/
 */
public class KeycloakAdminClient {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*");

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final AdapterConfig config;
    private Http http;
    private KeycloakTokenManager tokenManager;

    public KeycloakAdminClient(AdapterConfig config) {
        this.config = config;
    }

    public KeycloakAdminClient(InputStream config) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(VisibilityChecker.Std.defaultInstance()
                                                  .withFieldVisibility(JsonAutoDetect.Visibility.ANY));

        try {
            this.config = mapper.readValue(config, AdapterConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not parse configuration.", e);
        }
    }

    public AccessTokenResponse obtainAccessToken(String username, String password) {
        URI uri = KeycloakUriBuilder.fromUri(getConfig().getAuthServerUrl())
                                    .path(ServiceUrlConstants.TOKEN_PATH)
                                    .build(getRealm());
        HttpMethod<AccessTokenResponse> httpMethod = getHttp().post(uri);

        httpMethod = httpMethod.form()
                               .param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                               .param("username", username)
                               .param("password", password);

        if (getConfig().isPublicClient()) {
            httpMethod.param(OAuth2Constants.CLIENT_ID, getConfig().getResource());
        } else {
            httpMethod.authorizationBasic(getConfig().getResource(),
                                          getConfig().getCredentials().get("secret").toString());
        }

        return httpMethod.response().json(AccessTokenResponse.class).execute();
    }

    public UserInfo obtainUserInfo(String accessToken) {
        HttpMethod<UserInfo> httpMethod = getHttp().get("/realms/%s/protocol/openid-connect/userinfo", getRealm());

        httpMethod.authorizationBearer(accessToken);

        return httpMethod.response().json(UserInfo.class).execute();
    }

    public ClientRepresentation getRealmClient(String clientId) {
        HttpMethod<List<ClientRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/clients", getRealm());

        List<ClientRepresentation> clients = httpMethod.param("clientId", clientId)
                                                       .authentication()
                                                       .response()
                                                       .json(new TypeReference<List<ClientRepresentation>>() {})
                                                       .execute();

        return clients != null && !clients.isEmpty() ? clients.get(0) : null;
    }

    public UserRepresentation getUser(String userNameOrEmail) {
        if (!StringUtils.hasText(userNameOrEmail)) {
            return null;
        }

        HttpMethod<List<UserRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/users", getRealm());

        boolean isEmail = isEmail(userNameOrEmail);
        if (isEmail) {
            httpMethod.param("email", userNameOrEmail);
        } else {
            httpMethod.param("username", userNameOrEmail);
        }
        List<UserRepresentation> users = httpMethod.authentication()
                                                   .response()
                                                   .json(new TypeReference<List<UserRepresentation>>() {})
                                                   .execute();

        if (users != null) {
            for (UserRepresentation user : users) {
                // Note: We need to avoid someone try to register email as username to fake others.
                boolean matched = isEmail
                                  ? userNameOrEmail.equals(user.getEmail())
                                  : userNameOrEmail.equals(user.getUsername());
                if (matched) {
                    return user;
                }
            }
        }
        return null;
    }

    public List<UserRepresentation> getUsers() {
        HttpMethod<List<UserRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/users", getRealm());

        return httpMethod.authentication().response().json(new TypeReference<List<UserRepresentation>>() {}).execute();
    }

    public List<UserRepresentation> findUsers(String searchText) {
        HttpMethod<List<UserRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/users", getRealm());

        // https://github.com/keycloak/keycloak/blob/master/services/src/main/java/org/keycloak/services/resources/admin/UsersResource.java#L177
        return httpMethod.param("search", searchText)
                         .authentication()
                         .response()
                         .json(new TypeReference<List<UserRepresentation>>() {})
                         .execute();
    }

    public RoleRepresentation getRealmClientRoleByRoleName(String clientId, String roleName) {
        ClientRepresentation client = getRealmClient(clientId);
        HttpMethod<RoleRepresentation> httpMethod = getHttp().get("/admin/realms/%s/clients/%s/roles/%s",
                                                                  getRealm(),
                                                                  client.getId(),
                                                                  roleName);

        return httpMethod.authentication().response().json(RoleRepresentation.class).execute();
    }

    public RoleRepresentation getRealmRoleByRoleName(String roleName) {
        HttpMethod<RoleRepresentation> httpMethod = getHttp().get("/admin/realms/%s/roles/%s", getRealm(), roleName);

        return httpMethod.authentication().response().json(RoleRepresentation.class).execute();
    }

    public GroupRepresentation getRealmGroupByGroupPath(String groupPath) {
        String trimmedGroupPath = groupPath != null ? groupPath.trim() : null;
        if (trimmedGroupPath == null || trimmedGroupPath.isEmpty()) {
            return null;
        }

        List<GroupRepresentation> groups = getRealmGroups();
        for (GroupRepresentation group : groups) {
            if (group.getPath().equals(trimmedGroupPath)) {
                return group;
            }
        }
        return null;
    }

    public List<RoleRepresentation> getRealmClientRoles(String clientId) {
        ClientRepresentation client = getRealmClient(clientId);
        HttpMethod<List<RoleRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/clients/%s/roles",
                                                                        getRealm(),
                                                                        client.getId());

        return httpMethod.authentication().response().json(new TypeReference<List<RoleRepresentation>>() {}).execute();
    }

    public List<RoleRepresentation> getRealmRoles() {
        HttpMethod<List<RoleRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/roles", getRealm());

        return httpMethod.authentication().response().json(new TypeReference<List<RoleRepresentation>>() {}).execute();
    }

    public List<GroupRepresentation> getRealmGroups() {
        HttpMethod<List<GroupRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/groups", getRealm());

        List<GroupRepresentation> groups = httpMethod.authentication()
                                                     .response()
                                                     .json(new TypeReference<List<GroupRepresentation>>() {})
                                                     .execute();
        return getAllGroupsRecursively(groups);
    }

    public List<RoleRepresentation> getRealmClientRolesOfUser(String clientId, String username) {
        UserRepresentation user = getUser(username);
        return getRealmClientRolesOfUser(clientId, user);
    }

    public List<RoleRepresentation> getRealmClientRolesOfUser(String clientId, UserRepresentation user) {
        if (clientId == null || user == null) {
            return null;
        }

        // GET /{realm}/users/{id}/role-mappings/clients/{client}/composite
        // Get effective client-level roles including composite roles and normal roles.
        ClientRepresentation client = getRealmClient(clientId);
        HttpMethod<List<RoleRepresentation>> httpMethod = getHttp().get(
                "/admin/realms/%s/users/%s/role-mappings/clients/%s/composite/",
                getRealm(),
                user.getId(),
                client.getId());

        return httpMethod.authentication().response().json(new TypeReference<List<RoleRepresentation>>() {}).execute();
    }

    public List<RoleRepresentation> getRealmRolesOfUser(String username) {
        UserRepresentation user = getUser(username);
        return getRealmRolesOfUser(user);
    }

    public List<RoleRepresentation> getRealmRolesOfUser(UserRepresentation user) {
        if (user == null) {
            return null;
        }

        // GET /{realm}/users/{id}/role-mappings/realm/composite
        // Get effective realm-level roles including composite roles and normal roles.
        HttpMethod<List<RoleRepresentation>> httpMethod = getHttp().get(
                "/admin/realms/%s/users/%s/role-mappings/realm/composite/",
                getRealm(),
                user.getId());

        return httpMethod.authentication().response().json(new TypeReference<List<RoleRepresentation>>() {}).execute();
    }

    public List<GroupRepresentation> getRealmGroupsOfUser(UserRepresentation user) {
        if (user == null) {
            return null;
        }

        HttpMethod<List<GroupRepresentation>> httpMethod = getHttp().get("/admin/realms/%s/users/%s/groups",
                                                                         getRealm(),
                                                                         user.getId());

        return httpMethod.authentication().response().json(new TypeReference<List<GroupRepresentation>>() {}).execute();
    }

    public List<RoleRepresentation> combineRoles(Collection<RoleRepresentation>... collections) {
        List<RoleRepresentation> roles = new ArrayList<>();

        for (Collection<RoleRepresentation> collection : collections) {
            if (collection != null && !collection.isEmpty()) {
                roles.addAll(collection);
            }
        }
        return roles;
    }

    public List<GroupRepresentation> getAllGroupsRecursively(List<GroupRepresentation> groups) {
        List<GroupRepresentation> list = new ArrayList<>();
        if (groups == null || groups.isEmpty()) {
            return list;
        }

        for (GroupRepresentation group : groups) {
            list.add(group);
            list.addAll(getAllGroupsRecursively(group.getSubGroups()));
        }
        return list;
    }

    public boolean isEmail(String userNameOrEmail) {
        // https://github.com/keycloak/keycloak/blob/master/services/src/main/java/org/keycloak/services/resources/admin/UsersResource.java#L177
        return userNameOrEmail != null && EMAIL_PATTERN.matcher(userNameOrEmail).matches();
    }

    public String getRealm() {
        return getConfig().getRealm();
    }

    public AdapterConfig getConfig() {
        return this.config;
    }

    public synchronized Http getHttp() {
        if (this.http == null) {
            HttpClient httpClient = createHttpClient(getConfig());

            ClientAuthenticator clientAuthenticator = (HttpMethod httpMethod) -> {
                String token = getTokenManager().getAccessTokenString();

                httpMethod.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.ACCESS_TOKEN);
                httpMethod.authorizationBearer(token);
            };

            this.http = new Http(getConfig(), httpClient, clientAuthenticator);
        }

        return this.http;
    }

    private HttpClient createHttpClient(AdapterConfig config) {
        HttpClientBuilder builder = HttpClients.custom();

        if (config.isDisableTrustManager()) {
            try {
                builder.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                                                             .build());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        if (config.isAllowAnyHostname()) {
            builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        // Proxy url: http(s)://username:password@example.com/
        String proxyUrl = config.getProxyUrl() != null ? config.getProxyUrl().trim() : null;
        if (StringUtils.hasText(proxyUrl)) {
            String url = proxyUrl.replaceAll("://([^/]*@)?", "://");
            String auth = proxyUrl.replaceAll(".+://(([^/]*)@)?.+", "$2");

            HttpHost proxy = HttpHost.create(url);
            logger.info("Detect Keycloak behind the proxy '{}'", url);

            if (!auth.isEmpty()) {
                String user = auth.replaceAll(":.+$", "");
                String pass = auth.replaceAll("^.+:", "");

                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(proxy), new UsernamePasswordCredentials(user, pass));

                builder.setDefaultCredentialsProvider(credentialsProvider);
            }

            RequestConfig requestConfig = RequestConfig.custom().setProxy(proxy).build();
            builder.setDefaultRequestConfig(requestConfig);
        }

        return builder.build();
    }

    private KeycloakTokenManager getTokenManager() {
        if (this.tokenManager == null) {
            this.tokenManager = new KeycloakTokenManager(getConfig(), this.http);
        }

        return this.tokenManager;
    }
}
