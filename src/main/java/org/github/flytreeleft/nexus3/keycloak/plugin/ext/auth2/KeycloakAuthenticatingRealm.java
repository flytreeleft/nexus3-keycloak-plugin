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
package org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth2;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.sisu.Description;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.NexusKeycloakClientLoader;

@Singleton
@Named
@Description("Keycloak Authentication Realm (#2)")
public class KeycloakAuthenticatingRealm
        extends org.github.flytreeleft.nexus3.keycloak.plugin.KeycloakAuthenticatingRealm {
    public static final String NAME = KeycloakAuthenticatingRealm.class.getName();

    public KeycloakAuthenticatingRealm() {
        super(NexusKeycloakClientLoader.loadDefaultClient2());
    }

    @Override
    public String getName() {
        return NAME;
    }
}
