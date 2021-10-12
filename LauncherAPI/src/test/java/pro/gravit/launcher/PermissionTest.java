package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PermissionTest {
    @Test
    public void testPermission() {
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addAction("*");
            Assertions.assertTrue(permissions.hasAction("abcd"));
            Assertions.assertTrue(permissions.hasAction("t.a.c.d.f.*"));
            Assertions.assertTrue(permissions.hasAction("*"));
        }
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addAction("launchserver.*");
            Assertions.assertTrue(permissions.hasAction("launchserver.*"));
            Assertions.assertTrue(permissions.hasAction("launchserver.abcd"));
            Assertions.assertFalse(permissions.hasAction("default.abcd"));
            Assertions.assertFalse(permissions.hasAction("nolaunchserver.abcd"));
        }
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addAction("launchserver.*.prop");
            Assertions.assertTrue(permissions.hasAction("launchserver.ii.prop"));
            Assertions.assertTrue(permissions.hasAction("launchserver.ia.prop"));
            Assertions.assertFalse(permissions.hasAction("default.abcd"));
            Assertions.assertFalse(permissions.hasAction("launchserver.ia"));
            Assertions.assertFalse(permissions.hasAction("launchserver.ia.prop2"));
        }
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addAction("launchserver.*.def.*.prop");
            Assertions.assertTrue(permissions.hasAction("launchserver.1.def.2.prop"));
            Assertions.assertTrue(permissions.hasAction("launchserver.none.def.none.prop"));
            Assertions.assertTrue(permissions.hasAction("launchserver.def.def.def.prop"));
            Assertions.assertFalse(permissions.hasAction("launchserver.*.*.prop"));
            Assertions.assertFalse(permissions.hasAction("launchserver.*.undef.*.prop"));
        }
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addAction("launchserver.*.e.*.i.*.prop");
            Assertions.assertTrue(permissions.hasAction("launchserver.2.e.3.i.4.prop"));
            Assertions.assertTrue(permissions.hasAction("launchserver.12212.e.233455.i.2356436346346345345345345.prop"));
            Assertions.assertFalse(permissions.hasAction("launchserver.prop"));
        }
    }
}
