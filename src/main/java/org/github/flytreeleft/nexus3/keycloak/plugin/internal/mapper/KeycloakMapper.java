package org.github.flytreeleft.nexus3.keycloak.plugin.internal.mapper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.github.flytreeleft.nexus3.keycloak.plugin.internal.KeycloakUserManager;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserStatus;

public class KeycloakMapper {
    public static final String CLIENT_ROLE_PREFIX = "ClientRole";
    public static final String REALM_ROLE_PREFIX = "RealmRole";
    public static final String REALM_GROUP_PREFIX = "RealmGroup";

    public static User toUser(UserRepresentation representation) {
        if (representation == null) {
            return null;
        }

        User user = new User();
        user.setUserId(representation.getUsername());
        user.setFirstName(representation.getFirstName());
        user.setLastName(representation.getLastName());
        user.setEmailAddress(representation.getEmail());
        user.setReadOnly(true);
        user.setStatus(representation.isEnabled() ? UserStatus.active : UserStatus.disabled);
        user.setSource(KeycloakUserManager.SOURCE);
        return user;
    }

    public static Set<User> toUsers(List<UserRepresentation> representations) {
        Set<User> users = new LinkedHashSet<>();

        if (representations != null) {
            users.addAll(representations.stream().map(KeycloakMapper::toUser).collect(Collectors.toList()));
        }
        return users;
    }

    public static Role toRole(RoleRepresentation representation) {
        if (representation == null) {
            return null;
        }

        Role role = new Role();
        String prefix = representation.getClientRole() ? CLIENT_ROLE_PREFIX : REALM_ROLE_PREFIX;
        String roleName = String.format("%s:%s", prefix, representation.getName());

        // Use role name as role-id and role-name of Nexus3
        role.setRoleId(roleName);
        role.setName(roleName);
        if (representation.getDescription() != null && !representation.getDescription().isEmpty()) {
            role.setDescription(String.format("%s: %s", prefix, representation.getDescription()));
        }
        role.setReadOnly(true);
        role.setSource(KeycloakUserManager.SOURCE);

        return role;
    }

    public static Role toRole(GroupRepresentation representation) {
        if (representation == null) {
            return null;
        }

        Role role = new Role();
        String roleName = String.format("%s:%s", REALM_GROUP_PREFIX, representation.getPath());

        role.setRoleId(roleName);
        role.setName(roleName);
        role.setReadOnly(true);
        role.setSource(KeycloakUserManager.SOURCE);

        return role;
    }

    public static Set<Role> toRoles(List<?>... lists) {
        return toRoles(lists, false);
    }

    public static Set<String> toRoleIds(List<?>... lists) {
        return toRoleIds(lists, false);
    }

    /** Just for compatibility */
    public static Set<String> toCompatibleRoleIds(List<?>... lists) {
        return toRoleIds(lists, true);
    }

    private static Set<Role> toRoles(List<?>[] lists, boolean forCompatible) {
        Set<Role> roles = new LinkedHashSet<>();

        for (List<?> list : lists) {
            if (list == null || list.isEmpty()) {
                continue;
            }

            for (Object representation : list) {
                if (representation instanceof RoleRepresentation) {
                    if (forCompatible && ((RoleRepresentation) representation).getClientRole()) {
                        roles.add(toCompatibleRole((RoleRepresentation) representation));
                    }

                    roles.add(toRole((RoleRepresentation) representation));
                } else if (representation instanceof GroupRepresentation) {
                    roles.add(toRole((GroupRepresentation) representation));
                }
            }
        }
        return roles;
    }

    private static Set<String> toRoleIds(List<?>[] lists, boolean forCompatible) {
        Set<String> roleIds = new LinkedHashSet<>();
        roleIds.addAll(toRoles(lists, forCompatible).stream().map(Role::getRoleId).collect(Collectors.toList()));

        return roleIds;
    }

    private static Role toCompatibleRole(RoleRepresentation representation) {
        Role role = new Role();

        role.setRoleId(representation.getName());
        role.setName(representation.getName());
        role.setDescription(representation.getDescription());
        role.setReadOnly(true);
        role.setSource(KeycloakUserManager.SOURCE);
        return role;
    }
}
