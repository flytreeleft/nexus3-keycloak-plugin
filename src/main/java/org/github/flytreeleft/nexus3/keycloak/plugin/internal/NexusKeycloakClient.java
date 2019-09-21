package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.util.StringUtils;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.mapper.KeycloakMapper;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserSearchCriteria;

public class NexusKeycloakClient {
    private static final Logger logger = LoggerFactory.getLogger(NexusKeycloakClient.class);

    private String source;
    private String sourceCode;
    private File config;
    private transient KeycloakAdminClient keycloakAdminClient;

    public NexusKeycloakClient(String source) {
        this.source = source;
    }

    public NexusKeycloakClient(String source, String sourceCode, File config) {
        this.config = config;
        this.source = source;
        this.sourceCode = sourceCode;

        try {
            this.keycloakAdminClient = new KeycloakAdminClient(FileUtils.openInputStream(this.config));
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + this.config.getName(), e);
        }
    }

    public boolean authenticate(AuthenticationToken token) {
        if (token instanceof UsernamePasswordToken) {
            return authenticate((UsernamePasswordToken) token);
        } else if (token instanceof KeycloakHttpHeaderAuthToken) {
            return authenticate((KeycloakHttpHeaderAuthToken) token);
        }
        return false;
    }

    public boolean authenticate(UsernamePasswordToken token) {
        String principal = token.getUsername();
        String credentials = new String(token.getPassword());
        AccessTokenResponse accessTokenResponse = this.keycloakAdminClient.obtainAccessToken(principal, credentials);

        return accessTokenResponse != null && StringUtils.hasText(accessTokenResponse.getToken());
    }

    public boolean authenticate(KeycloakHttpHeaderAuthToken token) {
        String principal = token.getPrincipal();
        String credentials = token.getCredentials().toString();
        UserInfo userInfo = this.keycloakAdminClient.obtainUserInfo(credentials);

        return userInfo != null && userInfo.getPreferredUsername().equals(principal);
    }

    public Set<String> findRoleIdsByUserId(String userId) {
        String client = this.keycloakAdminClient.getConfig().getResource();
        UserRepresentation user = this.keycloakAdminClient.getUser(userId);

        List<RoleRepresentation> clientRoles = this.keycloakAdminClient.getRealmClientRolesOfUser(client, user);
        List<RoleRepresentation> realmRoles = this.keycloakAdminClient.getRealmRolesOfUser(user);
        List<GroupRepresentation> realmGroups = this.keycloakAdminClient.getRealmGroupsOfUser(user);

        // Convert to compatible roles to make sure the existing role-mappings are still working
        if (getSourceCode() == null) {
            return KeycloakMapper.toCompatibleRoleIds(getSource(), clientRoles, realmRoles, realmGroups);
        } else {
            return KeycloakMapper.toRoleIds(getSource(), getSourceCode(), clientRoles, realmRoles, realmGroups);
        }
    }

    public User findUserByUserId(String userId) {
        UserRepresentation user = this.keycloakAdminClient.getUser(userId);

        return KeycloakMapper.toUser(getSource(), user);
    }

    public Role findRoleByRoleId(String roleId) {
        String[] splits = roleId.split(":");
        String roleType = splits.length > 1 ? splits[0] : null;
        String roleSourceCode = splits.length > 2 ? splits[1] : null;
        String roleName = splits[splits.length - 1];

        if (!(roleSourceCode + "").equals(getSourceCode() + "")) {
            return null;
        }

        RoleRepresentation role;
        if (KeycloakMapper.REALM_GROUP_PREFIX.equals(roleType)) {
            GroupRepresentation group = this.keycloakAdminClient.getRealmGroupByGroupPath(roleName);

            return KeycloakMapper.toRole(getSource(), getSourceCode(), group);
        } else if (KeycloakMapper.REALM_ROLE_PREFIX.equals(roleType)) {
            role = this.keycloakAdminClient.getRealmRoleByRoleName(roleName);
        } else {
            String client = this.keycloakAdminClient.getConfig().getResource();
            role = this.keycloakAdminClient.getRealmClientRoleByRoleName(client, roleName);
        }

        return KeycloakMapper.toRole(getSource(), getSourceCode(), role);
    }

    public Set<String> findAllUserIds() {
        return findUsers().stream().map(User::getUserId).collect(Collectors.toSet());
    }

    public Set<User> findUsers() {
        List<UserRepresentation> users = this.keycloakAdminClient.getUsers();

        return KeycloakMapper.toUsers(getSource(), users);
    }

    public Set<User> findUserByCriteria(UserSearchCriteria criteria) {
        String search = "";

        if (StringUtils.hasText(criteria.getUserId())) {
            search = criteria.getUserId();
        } else if (StringUtils.hasText(criteria.getEmail())) {
            search = criteria.getEmail();
        }

        List<UserRepresentation> users = this.keycloakAdminClient.findUsers(search);
        if (users != null) {
            users = users.stream().filter(UserRepresentation::isEnabled).collect(Collectors.toList());
        }
        return KeycloakMapper.toUsers(getSource(), users);
    }

    public Set<Role> findRoles() {
        String client = this.keycloakAdminClient.getConfig().getResource();

        List<RoleRepresentation> clientRoles = this.keycloakAdminClient.getRealmClientRoles(client);
        List<RoleRepresentation> realmRoles = this.keycloakAdminClient.getRealmRoles();
        List<GroupRepresentation> realmGroups = this.keycloakAdminClient.getRealmGroups();

        return KeycloakMapper.toRoles(getSource(), getSourceCode(), clientRoles, realmRoles, realmGroups);
    }

    public String getSource() {
        return this.source;
    }

    public String getSourceCode() {
        return this.sourceCode;
    }
}
