package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.util.StringUtils;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.mapper.KeycloakMapper;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserSearchCriteria;

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

    public Set<String> findRoleIdsByUserId(String userId) {
        String client = getKeycloakAdminClient().getConfig().getResource();
        UserRepresentation user = getKeycloakAdminClient().getUser(userId);

        List<RoleRepresentation> clientRoles = getKeycloakAdminClient().getRealmClientRolesOfUser(client, user);
        List<RoleRepresentation> realmRoles = getKeycloakAdminClient().getRealmRolesOfUser(user);
        List<GroupRepresentation> realmGroups = getKeycloakAdminClient().getRealmGroupsOfUser(user);

        // Convert to compatible roles to make sure the existing role-mappings are still working
        return KeycloakMapper.toCompatibleRoleIds(clientRoles, realmRoles, realmGroups);
    }

    public User findUserByUserId(String userId) {
        UserRepresentation user = getKeycloakAdminClient().getUser(userId);

        return KeycloakMapper.toUser(user);
    }

    public Role findRoleByRoleId(String roleId) {
        RoleRepresentation role;

        if (roleId.startsWith(KeycloakMapper.REALM_GROUP_PREFIX)) {
            String groupPath = roleId.substring(KeycloakMapper.REALM_GROUP_PREFIX.length() + 1);
            GroupRepresentation group = getKeycloakAdminClient().getRealmGroupByGroupPath(groupPath);

            return KeycloakMapper.toRole(group);
        } else if (roleId.startsWith(KeycloakMapper.REALM_ROLE_PREFIX)) {
            String roleName = roleId.substring(KeycloakMapper.REALM_ROLE_PREFIX.length() + 1);
            role = getKeycloakAdminClient().getRealmRoleByRoleName(roleName);
        } else {
            String roleName = roleId.startsWith(KeycloakMapper.CLIENT_ROLE_PREFIX)
                              ? roleId.substring(KeycloakMapper.CLIENT_ROLE_PREFIX.length() + 1)
                              : roleId;
            String client = getKeycloakAdminClient().getConfig().getResource();
            role = getKeycloakAdminClient().getRealmClientRoleByRoleName(client, roleName);
        }
        return KeycloakMapper.toRole(role);
    }

    public Set<String> findAllUserIds() {
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
        List<RoleRepresentation> clientRoles = getKeycloakAdminClient().getRealmClientRoles(client);
        List<RoleRepresentation> realmRoles = getKeycloakAdminClient().getRealmRoles();
        List<GroupRepresentation> realmGroups = getKeycloakAdminClient().getRealmGroups();

        return KeycloakMapper.toRoles(clientRoles, realmRoles, realmGroups);
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
