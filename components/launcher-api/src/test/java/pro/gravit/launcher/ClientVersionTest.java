package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pro.gravit.launcher.base.profiles.ClientProfile;

public class ClientVersionTest {
    @Test
    public void parseTest() {
        Assertions.assertEquals("1.0.0", ClientProfile.Version.of("1.0.0").toCleanString());
        Assertions.assertEquals("1.0.0.1", ClientProfile.Version.of("1.0.0-1").toCleanString());
        Assertions.assertEquals("1.0.0", ClientProfile.Version.of("-----1.0.0").toCleanString());
    }

    @Test
    public void compareTest() {
        Assertions.assertEquals(0, ClientProfile.Version.of("1.0.0").compareTo(ClientProfile.Version.of("1.0.0")));
        Assertions.assertTrue(ClientProfile.Version.of("1.1.0").compareTo(ClientProfile.Version.of("1.0.0")) > 0);
        Assertions.assertTrue(ClientProfile.Version.of("2.0.0").compareTo(ClientProfile.Version.of("1.0.0")) > 0);
        Assertions.assertTrue(ClientProfile.Version.of("1.0.0").compareTo(ClientProfile.Version.of("1.0.1")) < 0);
        Assertions.assertTrue(ClientProfile.Version.of("1.1.0").compareTo(ClientProfile.Version.of("1.0.0")) > 0);
        Assertions.assertTrue(ClientProfile.Version.of("1.0.0").compareTo(ClientProfile.Version.of("1.1.0")) < 0);
        Assertions.assertEquals(0, ClientProfile.Version.of("1.0").compareTo(ClientProfile.Version.of("1.0.0")));
        Assertions.assertEquals(0, ClientProfile.Version.of("1.0.0").compareTo(ClientProfile.Version.of("1.0")));
        Assertions.assertTrue(ClientProfile.Version.of("1.0.1").compareTo(ClientProfile.Version.of("1.0")) > 0);
        Assertions.assertTrue(ClientProfile.Version.of("1.0").compareTo(ClientProfile.Version.of("1.0.1")) < 0);
        Assertions.assertTrue(ClientProfile.Version.of("1.0").compareTo(ClientProfile.Version.of("1.0.1")) < 0);
    }
}
