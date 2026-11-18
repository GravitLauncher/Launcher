package com.gravitlauncher.launchserver.cabinetstarter.embedded;

import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.LaunchServerBuilder;
import pro.gravit.launchserver.LaunchServerStarter;
import pro.gravit.launchserver.command.basic.DebugCommand;
import pro.gravit.launchserver.command.basic.StopCommand;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.holder.LaunchServerControlHolder;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.utils.command.StdCommandHandler;
import pro.gravit.utils.launch.ClassLoaderControl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class EmbeddedLaunchServer {
    private EmbeddedLaunchServer() {
    }

    public static AutoCloseable start(Path home, Path workDir, Path modulesDir, String apiUrl, String backendUrl, String apiKey,
                                      String projectName, Consumer<Boolean> setTrace, ClassLoaderControl control,
                                      Runnable stopCabinet) throws Exception {
        LaunchServerControlHolder.setControl(control);
        LaunchServerControlHolder.setController((ModuleLayer.Controller) control.getJava9ModuleController());
        var logging = CabinetLogging.initialize();
        try {
            var server = createServer(home, workDir, modulesDir, apiUrl, backendUrl, apiKey, projectName);
            try {
                var commands = server.commandHandler;
                var basic = commands.findCategory("basic").category;
                basic.unregisterCommand("debug");
                basic.registerCommand("debug", new DebugCommand(server, setTrace));
                basic.unregisterCommand("stop");
                basic.registerCommand("stop", new StopCommand(server, () -> {
                    commands.requestStop();
                    stopCabinet.run();
                }));
                server.modulesManager.fullInitializedLaunchServer(server);
                server.start();
                Thread console = Thread.ofPlatform().name("LauncherConsole").daemon().start(commands);
                return () -> {
                    try (logging) {
                        commands.requestStop();
                        console.interrupt();
                        server.close();
                    }
                };
            } catch (Exception | Error failure) {
                try { server.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
        } catch (Exception | Error failure) {
            try { logging.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private static LaunchServer createServer(Path home, Path workDir, Path modulesDir,
                                             String apiUrl, String backendUrl, String apiKey, String projectName) throws Exception {
        Files.createDirectories(workDir);

        var directories = new LaunchServer.LaunchServerDirectories();
        directories.dir = workDir;
        directories.modules = modulesDir;
        directories.librariesDir = home.resolve("lib");
        directories.launcherLibrariesDir = home.resolve(LaunchServer.LaunchServerDirectories.LAUNCHERLIBRARIES_NAME);
        directories.launcherLibrariesCompileDir = home.resolve(LaunchServer.LaunchServerDirectories.LAUNCHERLIBRARIESCOMPILE_NAME);
        directories.proguardDir = home.resolve(LaunchServer.LaunchServerDirectories.PROGUARD_DIR);
        directories.collect();

        var certificates = new CertificateManager();
        certificates.readTrustStore(directories.trustStore);

        var modulesManager = new LaunchServerModulesManager(modulesDir, workDir.resolve("config"), certificates.trustManager);
        try {
            Path modulesConfigFile = workDir.resolve("modules.json");
            var modulesConfig = LaunchServerStarter.loadLaunchServerModules(modulesManager, modulesConfigFile, modulesDir);
            modulesManager.initModules(null);
            LaunchServerStarter.registerAll();
            LaunchServerStarter.initGson(modulesManager);
            modulesManager.invokeEvent(new PreConfigPhase());

            Path configFile = workDir.resolve("LaunchServer.conf");
            if (!Files.exists(configFile)) {
                configFile = workDir.resolve("LaunchServer.json");
            }

            var env = LaunchServer.LaunchServerEnv.PRODUCTION;
            var configManager = new LaunchServerStarter.BasicLaunchServerConfigManager(configFile, modulesConfigFile) {
                @Override
                public LaunchServerConfig readConfig() throws IOException {
                    LaunchServerConfig config = super.readConfig();
                    if (config.configureEmbeddedBackend(apiUrl, backendUrl, apiKey)) {
                        config.verify();
                        writeConfig(config);
                    }
                    return config;
                }
            };
            if (!Files.exists(configFile)) {
                LaunchServerConfig defaults = LaunchServerConfig.getDefault(env);
                defaults.setProjectName(projectName);
                defaults.configureEmbeddedBackend(apiUrl, backendUrl, apiKey);
                configManager.writeConfig(defaults);
            }

            return new LaunchServerBuilder()
                    .setDirectories(directories)
                    .setEnv(env)
                    .setCommandHandler(new StdCommandHandler(true))
                    .setConfig(configManager.readConfig())
                    .setModulesManager(modulesManager)
                    .setLaunchServerConfigManager(configManager)
                    .setCertificateManager(certificates)
                    .setModulesConfig(modulesConfig)
                    .build();
        } catch (Exception | Error failure) {
            try { modulesManager.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
}
