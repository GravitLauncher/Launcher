package pro.gravit.launchserver;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launchserver.binary.PipelineContext;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.impl.TestLaunchServerConfigManager;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.manangers.LaunchServerGsonManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.StdCommandHandler;

import java.nio.file.Path;
import java.security.Security;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLifecycleTest {
    @TempDir Path dir;
    private LaunchServer server;

    @BeforeEach
    void prepare() throws Exception {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        var modules = new LaunchServerModulesManager(dir.resolve("modules"), dir.resolve("module-config"), null);
        Launcher.gsonManager = new LaunchServerGsonManager(modules);
        Launcher.gsonManager.initGson();
        var serverConfig = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
        var configManager = new TestLaunchServerConfigManager();
        server = new LaunchServerBuilder().setDir(dir)
                .setEnv(LaunchServer.LaunchServerEnv.TEST)
                .setConfig(serverConfig)
                .setCertificateManager(new CertificateManager())
                .setLaunchServerConfigManager(configManager)
                .setModulesManager(modules).setModulesConfig(configManager.modulesConfig)
                .setCommandHandler(new StdCommandHandler(false)).build();
    }

    @AfterEach
    void closeServer() throws Exception {
        if (server != null) server.close();
    }

    @Test
    void partialComponentInitializationClosesOnlyAttemptedHooksAndUnregistersAll() {
        var attempted = new TrackingComponent(true);
        var notAttempted = new TrackingComponent(false);
        LaunchServerConfig config = LaunchServerConfig.getDefault(LaunchServer.LaunchServerEnv.TEST);
        config.setLaunchServer(server);
        config.profilesProvider = null;
        config.updatesProvider = null;
        config.mirrors = new String[0];
        config.components = new LinkedHashMap<>();
        config.components.put("attempted", attempted);
        config.components.put("notAttempted", notAttempted);
        try {
            config.init(LaunchServer.ReloadType.FULL);
            assertThrows(IllegalStateException.class, config::initComponents);
        } finally {
            config.close(LaunchServer.ReloadType.FULL);
            config.close(LaunchServer.ReloadType.FULL);
        }

        assertEquals(1, attempted.closeCalls);
        assertEquals(0, notAttempted.closeCalls);
        assertFalse(server.reconfigurableManager.getCommands().containsKey("component.attempted"));
        assertFalse(server.reconfigurableManager.getCommands().containsKey("component.notattempted"));
    }

    @Test
    void launcherModuleCommandRegistersValidJarAndSkipsRejectedJar() throws Exception {
        var command = new pro.gravit.launchserver.command.modules.LoadLauncherModuleCommand(server);
        Path rejected = createModuleJar("rejected.jar", null);
        assertThrows(java.io.IOException.class, () -> command.invoke(rejected.toString()));
        assertTrue(server.modulesConfig.loadLauncherModules.isEmpty());

        Path accepted = createModuleJar("accepted.jar", "example.Module");
        command.invoke(accepted.toString());
        assertEquals(java.util.List.of(accepted.toString()), server.modulesConfig.loadLauncherModules);
        assertEquals(accepted, server.launcherModuleLoader.launcherModules.getFirst().path);
    }

    @Test
    void lateModuleReceivesLiveServerContext() throws Exception {
        Path jar = createModuleJar("late.jar", LateModule.class.getName());
        server.modulesManager.fullInitializedLaunchServer(server);

        new pro.gravit.launchserver.command.modules.LoadModuleCommand(server).invoke(jar.toString());

        var module = server.modulesManager.getModule(LateModule.class);
        assertSame(server, module.server);
        assertEquals(LauncherModule.InitStatus.FINISH, module.getInitStatus());
        Path rejected = createModuleJar("rejected.jar", null);
        assertThrows(java.io.IOException.class, () ->
                new pro.gravit.launchserver.command.modules.LoadModuleCommand(server).invoke(rejected.toString()));
        assertEquals(java.util.List.of(jar.toString()), server.modulesConfig.loadModules);
    }

    @Test
    void failedLauncherBuildClosesReaderClasspath() throws Exception {
        var task = new MainBuildTask(server) {
            @Override
            protected void initProps(pro.gravit.launchserver.binary.BuildContext context) {
                // This test exercises resource ownership, independent of launcher build configuration.
            }
        };
        var jar = new java.util.jar.JarFile(createModuleJar("reader.jar", null).toFile());
        task.reader.getCp().add(jar);
        task.preBuildHook.registerHook(context -> { throw new java.io.IOException("build failed"); });

        assertThrows(java.io.IOException.class, () -> task.process(new PipelineContext(server)));
        assertTrue(task.reader.getCp().isEmpty());
        assertThrows(IllegalStateException.class, jar::size);
    }

    private Path createModuleJar(String name, String mainClass) throws java.io.IOException {
        var manifest = new java.util.jar.Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        if (mainClass != null) manifest.getMainAttributes().putValue("Module-Main-Class", mainClass);
        Path jar = dir.resolve(name);
        try (var output = new java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jar), manifest)) {
            output.flush();
        }
        return jar;
    }

    public static class LateModule extends LauncherModule {
        private LaunchServer server;

        @Override
        public void init(LauncherInitContext context) {
            server = ((LaunchServerInitContext) context).server;
        }
    }

    private static final class TrackingComponent extends Component implements AutoCloseable, Reconfigurable {
        private final boolean failOnInit;
        private int closeCalls;

        private TrackingComponent(boolean failOnInit) {
            this.failOnInit = failOnInit;
        }

        @Override
        public void init(LaunchServer server) {
            if (failOnInit) throw new IllegalStateException("test init failure");
        }

        @Override
        public void close() {
            closeCalls++;
        }

        @Override
        public Map<String, Command> getCommands() {
            return Map.of();
        }
    }
}
