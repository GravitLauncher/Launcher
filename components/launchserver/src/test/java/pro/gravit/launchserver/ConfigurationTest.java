package pro.gravit.launchserver;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.impl.TestLaunchServerConfigManager;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.manangers.LaunchServerGsonManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.utils.command.StdCommandHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {
    @TempDir Path dir;
    private LaunchServer server;
    private TestLaunchServerConfigManager configManager;

    @BeforeEach
    void prepare() throws Exception {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        var modules = new LaunchServerModulesManager(dir.resolve("modules"), dir.resolve("config"), null);
        Launcher.gsonManager = new LaunchServerGsonManager(modules);
        Launcher.gsonManager.initGson();
        configManager = new TestLaunchServerConfigManager();
        server = new LaunchServerBuilder().setDir(dir)
                .setEnv(LaunchServer.LaunchServerEnv.TEST)
                .setConfig(LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST))
                .setCertificateManager(new CertificateManager())
                .setLaunchServerConfigManager(configManager)
                .setModulesManager(modules).setModulesConfig(configManager.modulesConfig)
                .setCommandHandler(new StdCommandHandler(false)).build();
    }

    @AfterEach
    void close() throws Exception {
        if (server != null) server.close();
    }

    @Test
    void invalidReloadLeavesActiveConfigurationUsable() throws Exception {
        var original = server.config;
        configManager.config.scheduler.threads = 0;
        assertThrows(IllegalStateException.class, () -> server.reload(LaunchServer.ReloadType.FULL));
        assertSame(original, server.config);
        assertFalse(server.service.isShutdown());
        server.start();
    }

    @Test
    void reloadWithoutComponentsRetainsInstancesAndRegistrations() throws Exception {
        var components = server.config.components;
        server.reload(LaunchServer.ReloadType.NO_COMPONENTS);
        assertSame(components, server.config.components);
        configManager.config = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
        server.reload(LaunchServer.ReloadType.NO_COMPONENTS);
        assertSame(components, server.config.components);
    }

    @Test
    void embeddedLifecycleDoesNotCreateControlSocketAndCloseIsIdempotent() throws Exception {
        server.start();
        assertFalse(Files.exists(server.controlFile));
        server.close();
        server.close();
        server.awaitTermination();
        assertTrue(server.service.isShutdown());
        assertThrows(IllegalStateException.class, server::start);
    }
}
