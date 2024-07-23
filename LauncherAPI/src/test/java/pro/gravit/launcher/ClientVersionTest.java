package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pro.gravit.launcher.base.profiles.ClientProfile;

public class ClientVersionTest {
    @Test
    public void parseTest() {
        Assertions.assertEquals(ClientProfile.Version.of("1.0.0").toCleanString(), "1.0.0");
        Assertions.assertEquals(ClientProfile.Version.of("1.0.0-1").toCleanString(), "1.0.0.1");
        Assertions.assertEquals(ClientProfile.Version.of("-----1.0.0").toCleanString(), "1.0.0");
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
