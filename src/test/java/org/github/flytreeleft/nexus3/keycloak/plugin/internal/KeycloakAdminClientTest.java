package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.github.flytreeleft.nexus3.keycloak.plugin.internal.http.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Ignore
public class KeycloakAdminClientTest {
    private static final String CONFIG_FILE = "keycloak.json";
    private static final String REALM_CLIENT = "nexus3";
    private static final String DEFAULT_USER = "a";
    private static final String DEFAULT_USER_PASSWORD = "a";

    private KeycloakAdminClient client;

    @Before
    public void before() throws IOException {
        InputStream config = Files.newInputStream(Paths.get(".", "etc", CONFIG_FILE));
        this.client = new KeycloakAdminClient(config);
    }

    @Test
    public void testObtainAccessToken() {
        AccessTokenResponse accessToken = this.client.obtainAccessToken(DEFAULT_USER, DEFAULT_USER_PASSWORD);
        Assert.assertNotNull(accessToken);

        try {
            accessToken = this.client.obtainAccessToken("unknown", "unknown");
            Assert.assertNull(accessToken);
        } catch (HttpResponseException e) {
            Assert.assertEquals(e.getStatusCode(), 401);
        }
    }

    @Test
    public void testGetRealmClient() {
        ClientRepresentation client = this.client.getRealmClient(REALM_CLIENT);
        Assert.assertNotNull(client);
    }

    @Test
    public void testGetUsers() {
        List<UserRepresentation> users = this.client.getUsers();
        Assert.assertTrue(users.size() > 0);
    }

    @Test
    public void testGetUser() {
        UserRepresentation user = this.client.getUser(DEFAULT_USER);
        Assert.assertNotNull(user);
    }

    @Test
    public void testFindUsers() {
        List<UserRepresentation> users = this.client.findUsers("");
        Assert.assertTrue(users.size() > 0);

        List<UserRepresentation> newUsers = this.client.findUsers(null);
        Assert.assertEquals(users.size(), newUsers.size());

        users = this.client.findUsers("unknown");
        Assert.assertTrue(users.isEmpty());

        users = this.client.findUsers(DEFAULT_USER);
        Assert.assertEquals(users.size(), 1);
    }

    @Test
    public void testGetRealmClientRoles() {
        List<RoleRepresentation> roles = this.client.getRealmClientRoles(REALM_CLIENT);
        Assert.assertTrue(roles.size() > 0);
    }

    @Test
    public void testGetRealmClientRole() {
        RoleRepresentation role = this.client.getRealmClientRole(REALM_CLIENT, "unknown");
        Assert.assertNull(role);

        role = this.client.getRealmClientRole(REALM_CLIENT, "uma_protection");
        Assert.assertNotNull(role);
    }

    @Test
    public void testGetRealmClientRolesOfUser() {
        List<RoleRepresentation> roles = this.client.getRealmClientRolesOfUser(REALM_CLIENT, DEFAULT_USER);
        Assert.assertTrue(roles.size() > 0);
    }
}
