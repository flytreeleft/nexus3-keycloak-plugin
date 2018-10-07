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
package org.github.flytreeleft.nexus3.keycloak.plugin;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.Description;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.NexusKeycloakClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
@Description("Keycloak Authentication Realm")
public class KeycloakAuthenticatingRealm extends AuthorizingRealm {
    public static final String NAME = KeycloakAuthenticatingRealm.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAuthenticatingRealm.class);

    private NexusKeycloakClient client;

    @Inject
    public KeycloakAuthenticatingRealm(final NexusKeycloakClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void onInit() {
        super.onInit();
        LOGGER.info("Keycloak Realm initialized...");
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = (String) principals.getPrimaryPrincipal();
//        LOGGER.info("doGetAuthorizationInfo for " + username);
        return new SimpleAuthorizationInfo(this.client.findRoleIdsByUserId(username));
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        if (!(token instanceof UsernamePasswordToken)) {
            throw new UnsupportedTokenException(String.format("Token of type %s  is not supported. A %s is required.",
                                                              token.getClass().getName(),
                                                              UsernamePasswordToken.class.getName()));
        }

        UsernamePasswordToken t = (UsernamePasswordToken) token;

        boolean authenticated = this.client.authenticate(t);
//        LOGGER.info("doGetAuthenticationInfo for " + t.getUsername() + ": " + authenticated);

        if (authenticated) {
            return createSimpleAuthInfo(t);
        } else {
            return null;
        }
    }

    /**
     * Creates the simple auth info.
     *
     * @param token
     *         the token
     * @return the simple authentication info
     */
    private SimpleAuthenticationInfo createSimpleAuthInfo(UsernamePasswordToken token) {
        return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), NAME);
    }
}
