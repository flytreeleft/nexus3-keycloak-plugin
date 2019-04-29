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

import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.github.flytreeleft.nexus3.keycloak.plugin.KeycloakAuthenticatingRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.AbstractReadOnlyUserManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;

@Singleton
@Named("Keycloak")
@Typed(UserManager.class)
public class KeycloakUserManager extends AbstractReadOnlyUserManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private NexusKeycloakClient client;

    public KeycloakUserManager() {
        this(NexusKeycloakClientLoader.loadDefaultClient());
    }

    public KeycloakUserManager(NexusKeycloakClient client) {
        this.logger.info(getClass().getName() + " is starting...");
        this.client = client;
    }

    @Override
    public String getAuthenticationRealmName() {
        return KeycloakAuthenticatingRealm.NAME;
    }

    @Override
    public String getSource() {
        return this.client.getSource();
    }

    @Override
    public Set<User> listUsers() {
        Set<User> users = this.client.findUsers();
        return users.stream().map(user -> completeUserRolesAndSource(user)).collect(Collectors.toSet());
    }

    @Override
    public Set<String> listUserIds() {
        return this.client.findAllUserIds();
    }

    @Override
    public Set<User> searchUsers(UserSearchCriteria criteria) {
        Set<User> users = this.client.findUserByCriteria(criteria);
        return users.stream().map(user -> completeUserRolesAndSource(user)).collect(Collectors.toSet());
    }

    @Override
    public User getUser(String userId) throws UserNotFoundException {
        User foundUser = this.client.findUserByUserId(userId);
        if (foundUser == null) {
            throw new UserNotFoundException(userId);
        }
        return completeUserRolesAndSource(foundUser);
    }

    private User completeUserRolesAndSource(User user) {
        user.setSource(getSource());

        Set<String> roles = this.client.findRoleIdsByUserId(user.getUserId());
        user.setRoles(roles.stream().map(role -> new RoleIdentifier(getSource(), role)).collect(Collectors.toSet()));
        return user;
    }
}
