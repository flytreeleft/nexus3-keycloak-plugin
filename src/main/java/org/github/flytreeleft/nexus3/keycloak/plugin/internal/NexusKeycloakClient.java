package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.util.StringUtils;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.mapper.KeycloakMapper;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserSearchCriteria;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Named("NexusKeycloakClient")
public class NexusKeycloakClient {
    private static final String CONFIG_FILE = "keycloak.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(NexusKeycloakClient.class);

    private transient KeycloakAdminClient keycloakAdminClient;

    public boolean authenticate(UsernamePasswordToken token) {
        AccessTokenResponse accessTokenResponse = getKeycloakAdminClient().obtainAccessToken(token.getUsername(),
                                                                                             new String(token.getPassword()));

        return accessTokenResponse != null && StringUtils.hasText(accessTokenResponse.getToken());
    }

    public Set<String> findRolesByUser(String username) {
        String client = getKeycloakAdminClient().getConfig().getResource();
        List<RoleRepresentation> roles = getKeycloakAdminClient().getRealmClientRolesOfUser(client, username);

        return KeycloakMapper.toRoleNames(roles);
    }

    public User findUserByUsername(String username) {
        UserRepresentation user = getKeycloakAdminClient().getUser(username);

        return KeycloakMapper.toUser(user);
    }

    public Role findRoleByRoleId(String roleId) {
        String client = getKeycloakAdminClient().getConfig().getResource();
        RoleRepresentation role = getKeycloakAdminClient().getRealmClientRole(client, roleId);

        return KeycloakMapper.toRole(role);
    }

    public Set<String> findAllUsernames() {
        return findUsers().stream().map(User::getUserId).collect(Collectors.toSet());
    }

    public Set<User> findUsers() {
        List<UserRepresentation> users = getKeycloakAdminClient().getUsers();

        return KeycloakMapper.toUsers(users);
    }

    public Set<User> findUserByCriteria(UserSearchCriteria criteria) {
        String search = "";

        if (StringUtils.hasText(criteria.getUserId())) {
            search = criteria.getUserId();
        } else if (StringUtils.hasText(criteria.getEmail())) {
            search = criteria.getEmail();
        }

        List<UserRepresentation> users = getKeycloakAdminClient().findUsers(search);
        if (users != null) {
            users = users.stream().filter(UserRepresentation::isEnabled).collect(Collectors.toList());
        }
        return KeycloakMapper.toUsers(users);
    }

    public Set<Role> findRoles() {
        String client = getKeycloakAdminClient().getConfig().getResource();
        List<RoleRepresentation> roles = getKeycloakAdminClient().getRealmClientRoles(client);

        return KeycloakMapper.toRoles(roles);
    }

    private synchronized KeycloakAdminClient getKeycloakAdminClient() {
        if (this.keycloakAdminClient == null) {
            try {
                this.keycloakAdminClient = new KeycloakAdminClient(getKeycloakJson());
            } catch (IOException e) {
                throw new RuntimeException("Could not read keycloak.json", e);
            }
        }
        return this.keycloakAdminClient;
    }

    private InputStream getKeycloakJson() throws IOException {
        return Files.newInputStream(Paths.get(".", "etc", CONFIG_FILE));
    }
}
