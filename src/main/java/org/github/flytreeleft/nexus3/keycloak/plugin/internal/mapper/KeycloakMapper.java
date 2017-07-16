package org.github.flytreeleft.nexus3.keycloak.plugin.internal.mapper;

import org.github.flytreeleft.nexus3.keycloak.plugin.internal.KeycloakUserManager;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserStatus;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KeycloakMapper {

    public static User toUser(UserRepresentation u) {
        if (u == null) {
            return null;
        }

        User user = new User();
        user.setUserId(u.getUsername());
        user.setFirstName(u.getFirstName());
        user.setLastName(u.getLastName());
        user.setEmailAddress(u.getEmail());
        user.setReadOnly(true);
        user.setStatus(u.isEnabled() ? UserStatus.active : UserStatus.disabled);
        user.setSource(KeycloakUserManager.SOURCE);
        return user;
    }

    public static Set<User> toUsers(List<UserRepresentation> l) {
        return l != null ? l.stream().map(KeycloakMapper::toUser).collect(Collectors.toSet()) : Collections.emptySet();
    }

    public static Role toRole(RoleRepresentation r) {
        if (r == null) {
            return null;
        }

        Role role = new Role();
        role.setRoleId(r.getName());
        role.setName(r.getName());
        role.setDescription(r.getDescription());
        role.setReadOnly(true);
        role.setSource(KeycloakUserManager.SOURCE);
        return role;
    }

    public static Set<Role> toRoles(List<RoleRepresentation> l) {
        return l != null ? l.stream().map(KeycloakMapper::toRole).collect(Collectors.toSet()) : Collections.emptySet();
    }

    public static Set<String> toRoleNames(List<RoleRepresentation> l) {
        return l != null
               ? l.stream().map(RoleRepresentation::getName).collect(Collectors.toSet())
               : Collections.emptySet();
    }
}
