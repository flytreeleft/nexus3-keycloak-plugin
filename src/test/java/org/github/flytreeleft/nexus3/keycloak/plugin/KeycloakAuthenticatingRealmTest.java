package org.github.flytreeleft.nexus3.keycloak.plugin;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.github.flytreeleft.nexus3.keycloak.plugin.internal.NexusKeycloakClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

public class KeycloakAuthenticatingRealmTest {
    private KeycloakAuthenticatingRealm r;
    private NexusKeycloakClient mockedClient;

    @Before
    public void before() {
        this.mockedClient = Mockito.mock(NexusKeycloakClient.class);
        this.r = new KeycloakAuthenticatingRealm(this.mockedClient);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals(KeycloakAuthenticatingRealm.NAME, r.getName());
    }

    @Test
    public void testDoGetAuthorizationInfoPrincipalCollection() {
        PrincipalCollection principals = new SimplePrincipalCollection("Test1", KeycloakAuthenticatingRealm.NAME);
        Mockito.when(this.mockedClient.findRolesByUser("Test1")).thenReturn(fakeAuths());
        AuthorizationInfo info = this.r.doGetAuthorizationInfo(principals);
        Assert.assertEquals(2, info.getRoles().size());
        Assert.assertTrue(info.getRoles().contains("role1"));
        Assert.assertTrue(info.getRoles().contains("role2"));
    }

    private Set<String> fakeAuths() {
        Set<String> auths = new HashSet<>();
        auths.add("role1");
        auths.add("role2");
        return auths;
    }

    @Test
    public void testDoGetAuthenticationInfoNoKeycloak() {
        AuthenticationToken token = new UsernamePasswordToken("u1", new char[] { 'p', '1' });
        AuthenticationInfo info = this.r.doGetAuthenticationInfo(token);
        Assert.assertNull(info);
    }

    @Test
    public void testDoGetAuthenticationInfoWithKeycloakOK() {
        UsernamePasswordToken token = new UsernamePasswordToken("u1", new char[] { 'p', '1' });
        Mockito.when(this.mockedClient.authenticate(token)).thenReturn(true);
        AuthenticationInfo info = this.r.doGetAuthenticationInfo(token);
        Assert.assertNotNull(info);
    }
}
