package pro.gravit.launchserver;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.manangers.LaunchServerGsonManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;

import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigMigrationTest {
    @TempDir
    static Path tempDir;

    @BeforeAll
    static void initGson() throws Exception {
        Path modulesDir = Files.createDirectories(tempDir.resolve("modules"));
        Path configDir = Files.createDirectories(tempDir.resolve("config"));
        LaunchServerModulesManager modulesManager = new LaunchServerModulesManager(modulesDir, configDir, null);
        Launcher.gsonManager = new LaunchServerGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
    }

    @Test
    void rejectsLegacyNettyConfig() {
        assertThrows(IllegalStateException.class,
                () -> LaunchServerConfig.readConfig(new StringReader("{\"netty\":{}}")));
    }

    @Test
    void rejectsRemovedHttpSwitch() {
        assertThrows(IllegalStateException.class,
                () -> LaunchServerConfig.readConfig(new StringReader("{\"launcher\":{\"experimentalDevOnlyHttpApi\":true}}")));
    }

    @Test
    void rejectsWebSocketApiMode() {
        assertThrows(IllegalStateException.class,
                () -> LaunchServerConfig.readConfig(new StringReader("{\"launcher\":{\"api\":{\"mode\":\"WEBSOCKET\"}}}")));
    }

    @Test
    void rejectsMicrosoftClientSecret() {
        assertThrows(IllegalStateException.class,
                () -> LaunchServerConfig.readConfig(new StringReader("{\"launcher\":{\"api\":{\"microsoft\":{\"clientSecret\":\"old-secret\"}}}}")));
    }
}
