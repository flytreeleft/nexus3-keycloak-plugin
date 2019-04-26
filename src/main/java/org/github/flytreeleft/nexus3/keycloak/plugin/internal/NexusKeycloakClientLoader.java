package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserSearchCriteria;

public class NexusKeycloakClientLoader {
    private static final String PLUGIN_CONFIG_FILE = "keycloak-plugin.properties";
    private static final String CONFIG_FILE = "keycloak.json";

    private static final String PLUGIN_CONFIG_KEY_KEYCLOAK_AUTH_CONFIG = "keycloak.auth.config";

    private static final Logger LOGGER = LoggerFactory.getLogger(NexusKeycloakClientLoader.class);

    private static NexusKeycloakClient client;

    public static NexusKeycloakClient loadClient() {
        if (client == null) {
            Properties props = getPluginProperties();
            String keycloakJSON = props.getProperty(PLUGIN_CONFIG_KEY_KEYCLOAK_AUTH_CONFIG, "").trim();
            String[] jsonFiles = keycloakJSON.isEmpty()
                                 ? new String[] { CONFIG_FILE }
                                 : keycloakJSON.split("\\s*(,|;)\\s*");

            if (jsonFiles.length == 1) {
                client = new NexusKeycloakClient(Paths.get(".", "etc", jsonFiles[0]));

                LOGGER.info(String.format("Create NexusKeycloakClient with %s", jsonFiles[0]));
            } else {
                List<NexusKeycloakClient> clients = new ArrayList<>();
                for (String jsonFile : jsonFiles) {
                    NexusKeycloakClient c = new NexusKeycloakClient(Paths.get(".", "etc", jsonFile), true);
                    clients.add(c);
                }

                client = new CompositeNexusKeycloakClient(clients);

                LOGGER.info(String.format("Create CompositeNexusKeycloakClient with %d keycloak.json files: %s",
                                          jsonFiles.length,
                                          keycloakJSON));
            }
        }
        return client;
    }

    public static Properties getPluginProperties() {
        Properties props = new Properties();

        File config = FileUtils.getFile(".", "etc", PLUGIN_CONFIG_FILE);
        if (config.exists()) {
            try {
                props.load(FileUtils.openInputStream(config));
            } catch (IOException e) {
                throw new IllegalStateException("Can not read the plugin properties", e);
            }
        }
        return props;
    }

    static class CompositeNexusKeycloakClient extends NexusKeycloakClient {
        private List<NexusKeycloakClient> clients;

        public CompositeNexusKeycloakClient(List<NexusKeycloakClient> clients) {
            super(null);
            this.clients = clients != null ? clients : new ArrayList<>();
        }

        @Override
        public boolean authenticate(UsernamePasswordToken token) {
            for (NexusKeycloakClient client : this.clients) {
                // Do authenticate for the first matching
                if (client.findUserByUserId(token.getUsername()) != null) {
                    return client.authenticate(token);
                }
            }
            return false;
        }

        @Override
        public Set<String> findRoleIdsByUserId(String userId) {
            for (NexusKeycloakClient client : this.clients) {
                if (client.findUserByUserId(userId) != null) {
                    return client.findRoleIdsByUserId(userId);
                }
            }
            return new HashSet<>();
        }

        @Override
        public User findUserByUserId(String userId) {
            for (NexusKeycloakClient client : this.clients) {
                User user = client.findUserByUserId(userId);
                if (user != null) {
                    return user;
                }
            }
            return null;
        }

        @Override
        public Role findRoleByRoleId(String roleId) {
            for (NexusKeycloakClient client : this.clients) {
                Role role = client.findRoleByRoleId(roleId);
                if (role != null) {
                    return role;
                }
            }
            return null;
        }

        @Override
        public Set<String> findAllUserIds() {
            Set<String> userIds = new HashSet<>();

            for (NexusKeycloakClient client : this.clients) {
                Set<String> set = client.findAllUserIds();
                // Remove the existing users
                set.removeAll(userIds);
                // Add the new users to the result
                userIds.addAll(set);
            }
            return userIds;
        }

        @Override
        public Set<User> findUsers() {
            Set<User> users = new HashSet<>();

            for (NexusKeycloakClient client : this.clients) {
                Set<User> set = client.findUsers();
                // Remove the existing users
                set.removeAll(users);
                // Add the new users to the result
                users.addAll(set);
            }
            return users;
        }

        @Override
        public Set<User> findUserByCriteria(UserSearchCriteria criteria) {
            Set<User> users = new HashSet<>();

            for (NexusKeycloakClient client : this.clients) {
                Set<User> set = client.findUserByCriteria(criteria);
                // Remove the existing users
                set.removeAll(users);
                // Add the new users to the result
                users.addAll(set);
            }
            return users;
        }

        @Override
        public Set<Role> findRoles() {
            Set<Role> roles = new HashSet<>();

            for (NexusKeycloakClient client : this.clients) {
                Set<Role> set = client.findRoles();
                // Remove the existing roles
                set.removeAll(roles);
                // Add the new roles to the result
                roles.addAll(set);
            }
            return roles;
        }
    }
}
