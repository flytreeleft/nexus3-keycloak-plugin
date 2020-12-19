package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserSearchCriteria;

public class NexusKeycloakClientLoader {
    public static final String DEFAULT_CONFIG = "keycloak.json";
    public static final String DEFAULT_0_CONFIG = "keycloak.0.json";
    public static final String DEFAULT_1_CONFIG = "keycloak.1.json";
    public static final String DEFAULT_2_CONFIG = "keycloak.2.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(NexusKeycloakClientLoader.class);

    private static final Map<String, NexusKeycloakClient> clientMap = new HashMap<>();

    public static NexusKeycloakClient loadDefaultClient() {
        return loadClient("Keycloak", null, DEFAULT_CONFIG);
    }

    public static NexusKeycloakClient loadDefaultClient0() {
        return loadClient("Keycloak (#0)", "kc0", DEFAULT_0_CONFIG);
    }

    public static NexusKeycloakClient loadDefaultClient1() {
        return loadClient("Keycloak (#1)", "kc1", DEFAULT_1_CONFIG);
    }

    public static NexusKeycloakClient loadDefaultClient2() {
        return loadClient("Keycloak (#2)", "kc2", DEFAULT_2_CONFIG);
    }

    public synchronized static NexusKeycloakClient loadClient(
            String source, String sourceCode, String keycloakConfigName
    ) {
        NexusKeycloakClient client = clientMap.get(keycloakConfigName);

        if (client == null) {
            LOGGER.debug("Attempting to instantiate new client...");
            File config = FileUtils.getFile(".", "etc", keycloakConfigName);
            if (config.exists()) {
                client = new NexusKeycloakClient(source, sourceCode, config);

                clientMap.put(keycloakConfigName, client);
            } else {
                LOGGER.debug(config.getAbsolutePath() + " file not found, will create no-op client");
            }
        }
        return client != null ? client : new NoopNexusKeycloakClient(source);
    }

    static class NoopNexusKeycloakClient extends NexusKeycloakClient {

        public NoopNexusKeycloakClient(String source) {
            super(source);
        }

        @Override
        public boolean authenticate(AuthenticationToken token) {
            return false;
        }

        @Override
        public Set<String> findRoleIdsByUserId(String userId) {
            return new HashSet<>();
        }

        @Override
        public User findUserByUserId(String userId) {
            return null;
        }

        @Override
        public Role findRoleByRoleId(String roleId) {
            return null;
        }

        @Override
        public Set<String> findAllUserIds() {
            return new HashSet<>();
        }

        @Override
        public Set<User> findUsers() {
            return new HashSet<>();
        }

        @Override
        public Set<User> findUserByCriteria(UserSearchCriteria criteria) {
            return new HashSet<>();
        }

        @Override
        public Set<Role> findRoles() {
            return new HashSet<>();
        }
    }
}
