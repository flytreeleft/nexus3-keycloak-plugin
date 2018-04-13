package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.github.flytreeleft.nexus3.keycloak.plugin.KeycloakAuthenticatingRealm;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KeycloakUserManagerTest {
    private KeycloakUserManager m;
    private NexusKeycloakClient mockedClient;

    @Before
    public void before() {
        this.mockedClient = mock(NexusKeycloakClient.class);
        this.m = new KeycloakUserManager(this.mockedClient);
    }

    @Test
    public void testGetAuthenticationRealmName() {
        Assert.assertEquals(KeycloakAuthenticatingRealm.NAME, m.getAuthenticationRealmName());
    }

    @Test
    public void testGetSource() {
        Assert.assertEquals("Keycloak", m.getSource());
    }

    @Test
    public void testListUsers() {
        when(this.mockedClient.findUsers()).thenReturn(mockedUsers());
        Assert.assertEquals(2, this.m.listUsers().size());
    }

    private Set<User> mockedUsers() {
        Set<User> u = new HashSet<>();
        User u1 = new User();
        User u2 = new User();
        u1.setUserId("1");
        u2.setUserId("2");
        u.add(u1);
        u.add(u2);
        return u;
    }

    @Test
    public void testListUserIds() {
        when(this.mockedClient.findAllUsernames()).thenReturn(mockedUsers().stream()
                                                                           .map(User::getUserId)
                                                                           .collect(Collectors.toSet()));
        Assert.assertEquals(2, this.m.listUserIds().size());
    }

    @Test
    public void testSearchUsers() {
        UserSearchCriteria usc = new UserSearchCriteria("1");
        when(this.mockedClient.findUserByCriteria(usc)).thenReturn(mockedUsers());
        Assert.assertEquals(2, this.m.searchUsers(usc).size());
    }

    @Test(expected = UserNotFoundException.class)
    public void testNotFoundUser() throws Exception {
        this.m.getUser("42");
    }

    @Test
    public void testGetUser() throws UserNotFoundException {
        when(this.mockedClient.findUserByUsername("1")).thenReturn(mockedUsers().iterator().next());
        User u = mockedUsers().iterator().next();
        u.setSource("Keycloak");
        Assert.assertEquals(u, this.m.getUser("1"));
    }
}
