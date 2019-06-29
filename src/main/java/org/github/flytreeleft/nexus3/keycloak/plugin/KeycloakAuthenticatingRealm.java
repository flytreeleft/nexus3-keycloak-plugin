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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.Description;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.KeycloakHttpHeaderAuthToken;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.NexusKeycloakClient;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.NexusKeycloakClientLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
@Description("Keycloak Authentication Realm")
public class KeycloakAuthenticatingRealm extends AuthorizingRealm {
    public static final String NAME = KeycloakAuthenticatingRealm.class.getName();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private NexusKeycloakClient client;

    public KeycloakAuthenticatingRealm() {
        this(NexusKeycloakClientLoader.loadDefaultClient());
    }

    public KeycloakAuthenticatingRealm(NexusKeycloakClient client) {
        this.client = client;
        //setCredentialsMatcher((token, info) -> true);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void onInit() {
        super.onInit();
        this.logger.info(String.format("Keycloak Realm %s initialized...", getClass().getName()));
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return (token instanceof UsernamePasswordToken) || (token instanceof KeycloakHttpHeaderAuthToken);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = (String) principals.getPrimaryPrincipal();
        if (!principals.getRealmNames().contains(getName())) {
            return null;
        }

        this.logger.info("doGetAuthorizationInfo for " + username);

        return new SimpleAuthorizationInfo(this.client.findRoleIdsByUserId(username));
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        boolean authenticated = false;

        try {
            authenticated = this.client.authenticate(token);

            this.logger.info("doGetAuthenticationInfo for {} via {}: {}",
                             token.getPrincipal(),
                             token.getClass().getName(),
                             authenticated);
        } catch (RuntimeException e) {
            this.logger.info("doGetAuthenticationInfo failed: " + e.getMessage(), e);
        }

        if (authenticated) {
            return createSimpleAuthInfo(token);
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
    private SimpleAuthenticationInfo createSimpleAuthInfo(AuthenticationToken token) {
        return new SimpleAuthenticationInfo(token.getPrincipal(), token.getCredentials(), getName());
    }
}
