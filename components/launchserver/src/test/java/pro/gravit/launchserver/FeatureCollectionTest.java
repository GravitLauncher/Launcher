package pro.gravit.launchserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.Feature;

public class FeatureCollectionTest {
    public static class TestClass1 implements TextInterface1 {

    }

    @Feature("test")
    public interface TextInterface1 {

    }

    @Test
    public void simpleTest() {
        var set = AuthProviderPair.getFeatures(TestClass1.class);
        Assertions.assertTrue(set.contains("test"));
    }
}
