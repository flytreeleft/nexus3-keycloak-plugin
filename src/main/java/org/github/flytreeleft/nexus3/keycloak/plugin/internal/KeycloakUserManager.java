/*
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import com.google.inject.Inject;
import org.github.flytreeleft.nexus3.keycloak.plugin.KeycloakAuthenticatingRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.AbstractReadOnlyUserManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Typed(UserManager.class)
@Named("Keycloak")
public class KeycloakUserManager extends AbstractReadOnlyUserManager {
    public static final String SOURCE = "Keycloak";
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakUserManager.class);

    private NexusKeycloakClient client;

    @Inject
    public KeycloakUserManager(NexusKeycloakClient client) {
        LOGGER.info("KeycloakUserManager is starting...");
        this.client = client;
    }

    @Override
    public String getAuthenticationRealmName() {
        return KeycloakAuthenticatingRealm.NAME;
    }

    @Override
    public String getSource() {
        return SOURCE;
    }

    private User completeUserRolesAndSource(User user) {
        user.setSource(SOURCE);
        Set<String> roles = this.client.findRolesByUser(user.getUserId());
        user.setRoles(roles.stream().map(r -> new RoleIdentifier(SOURCE, r)).collect(Collectors.toSet()));
        return user;
    }

    @Override
    public Set<User> listUsers() {
        return this.client.findUsers().stream().map(u -> completeUserRolesAndSource(u)).collect(Collectors.toSet());
    }

    @Override
    public Set<String> listUserIds() {
        return this.client.findAllUsernames();
    }

    @Override
    public Set<User> searchUsers(UserSearchCriteria criteria) {
        return this.client.findUserByCriteria(criteria)
                     .stream()
                     .map(u -> completeUserRolesAndSource(u))
                     .collect(Collectors.toSet());
    }

    @Override
    public User getUser(String userId) throws UserNotFoundException {
        User u = this.client.findUserByUsername(userId);
        return completeUserRolesAndSource(u);
    }
}
