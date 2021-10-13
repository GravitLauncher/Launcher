package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PermissionTest {
    @Test
    public void testPermission() {
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addPerm("*");
            Assertions.assertTrue(permissions.hasPerm("abcd"));
            Assertions.assertTrue(permissions.hasPerm("t.a.c.d.f.*"));
            Assertions.assertTrue(permissions.hasPerm("*"));
        }
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addPerm("launchserver.*");
            Assertions.assertTrue(permissions.hasPerm("launchserver.*"));
            Assertions.assertTrue(permissions.hasPerm("launchserver.abcd"));
            Assertions.assertFalse(permissions.hasPerm("default.abcd"));
            Assertions.assertFalse(permissions.hasPerm("nolaunchserver.abcd"));
        }
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addPerm("launchserver.*.prop");
            Assertions.assertTrue(permissions.hasPerm("launchserver.ii.prop"));
            Assertions.assertTrue(permissions.hasPerm("launchserver.ia.prop"));
            Assertions.assertFalse(permissions.hasPerm("default.abcd"));
            Assertions.assertFalse(permissions.hasPerm("launchserver.ia"));
            Assertions.assertFalse(permissions.hasPerm("launchserver.ia.prop2"));
        }
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addPerm("launchserver.*.def.*.prop");
            Assertions.assertTrue(permissions.hasPerm("launchserver.1.def.2.prop"));
            Assertions.assertTrue(permissions.hasPerm("launchserver.none.def.none.prop"));
            Assertions.assertTrue(permissions.hasPerm("launchserver.def.def.def.prop"));
            Assertions.assertFalse(permissions.hasPerm("launchserver.*.*.prop"));
            Assertions.assertFalse(permissions.hasPerm("launchserver.*.undef.*.prop"));
        }
        {
            ClientPermissions permissions = new ClientPermissions();
            permissions.addPerm("launchserver.*.e.*.i.*.prop");
            Assertions.assertTrue(permissions.hasPerm("launchserver.2.e.3.i.4.prop"));
            Assertions.assertTrue(permissions.hasPerm("launchserver.12212.e.233455.i.2356436346346345345345345.prop"));
            Assertions.assertFalse(permissions.hasPerm("launchserver.prop"));
        }
    }
}
