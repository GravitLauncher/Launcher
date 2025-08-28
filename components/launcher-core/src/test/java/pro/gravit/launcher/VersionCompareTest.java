package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pro.gravit.utils.Version;


public class VersionCompareTest {
    @Test
    public void compareTest() {
        Assertions.assertTrue(new Version(1, 0, 0).compareTo(new Version(1, 1, 0)) < 0);
        Assertions.assertTrue(new Version(1, 1, 0).compareTo(new Version(1, 0, 0)) > 0);
        Assertions.assertTrue(new Version(1, 0, 0).isLowerThan(new Version(1, 1, 0)));
        Assertions.assertTrue(new Version(1, 0, 1).isUpperThan(new Version(1, 0, 0)));
    }
}
