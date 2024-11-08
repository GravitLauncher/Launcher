package pro.gravit.launchserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.mix.MixProvider;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.auth.profiles.ProfileProvider;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.manangers.LaunchServerGsonManager;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.command.JLineCommandHandler;
import pro.gravit.utils.command.StdCommandHandler;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public class LaunchServerStarter {
    public static final boolean allowUnsigned = Boolean.getBoolean("launchserver.allowUnsigned");
    public static final boolean prepareMode = Boolean.getBoolean("launchserver.prepareMode");
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        JVMHelper.verifySystemProperties(LaunchServer.class, false);
        //LogHelper.addOutput(IOHelper.WORKING_DIR.resolve("LaunchServer.log"));
        LogHelper.printVersion("LaunchServer");
        LogHelper.printLicense("LaunchServer");
        Path dir = IOHelper.WORKING_DIR;
        Path configFile, runtimeConfigFile;
        try {
            Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider(new BouncyCastleProvider());
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            LogHelper.error("Library BouncyCastle not found! Is directory 'libraries' empty?");
            return;
        }
        LaunchServer.LaunchServerDirectories directories = new LaunchServer.LaunchServerDirectories();
        directories.dir = dir;
        directories.collect();
        CertificateManager certificateManager = new CertificateManager();
        try {
            certificateManager.readTrustStore(dir.resolve("truststore"));
        } catch (CertificateException e) {
            throw new IOException(e);
        }
        {
            LauncherTrustManager.CheckClassResult result = certificateManager.checkClass(LaunchServer.class);
            if (result.type == LauncherTrustManager.CheckClassResultType.SUCCESS) {
                logger.info("LaunchServer signed by {}", result.endCertificate.getSubjectX500Principal().getName());
            } else if (result.type == LauncherTrustManager.CheckClassResultType.NOT_SIGNED) {
                // None
            } else {
                if (result.exception != null) {
                    logger.error(result.exception);
                }
                logger.warn("LaunchServer signed incorrectly. Status: {}", result.type.name());
            }
        }

        LaunchServerRuntimeConfig runtimeConfig;
        LaunchServerConfig config;
        LaunchServer.LaunchServerEnv env = LaunchServer.LaunchServerEnv.PRODUCTION;
        LaunchServerModulesManager modulesManager = new LaunchServerModulesManager(directories.modules, dir.resolve("config"), certificateManager.trustManager);
        modulesManager.autoload();
        modulesManager.initModules(null);
        registerAll();
        initGson(modulesManager);
        printExperimentalBranch();
        if (IOHelper.exists(dir.resolve("LaunchServer.conf"))) {
            configFile = dir.resolve("LaunchServer.conf");
        } else {
            configFile = dir.resolve("LaunchServer.json");
        }
        if (IOHelper.exists(dir.resolve("RuntimeLaunchServer.conf"))) {
            runtimeConfigFile = dir.resolve("RuntimeLaunchServer.conf");
        } else {
            runtimeConfigFile = dir.resolve("RuntimeLaunchServer.json");
        }
        CommandHandler localCommandHandler;
        try {
            Class.forName("org.jline.terminal.Terminal");

            // JLine2 available
            localCommandHandler = new JLineCommandHandler();
            logger.info("JLine2 terminal enabled");
        } catch (ClassNotFoundException ignored) {
            localCommandHandler = new StdCommandHandler(true);
            logger.warn("JLine2 isn't in classpath, using std");
        }
        modulesManager.invokeEvent(new PreConfigPhase());
        generateConfigIfNotExists(configFile, localCommandHandler, env);
        logger.info("Reading LaunchServer config file");
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = Launcher.gsonManager.gson.fromJson(reader, LaunchServerConfig.class);
        }
        if (!Files.exists(runtimeConfigFile)) {
            logger.info("Reset LaunchServer runtime config file");
            runtimeConfig = new LaunchServerRuntimeConfig();
            runtimeConfig.reset();
        } else {
            logger.info("Reading LaunchServer runtime config file");
            try (BufferedReader reader = IOHelper.newReader(runtimeConfigFile)) {
                runtimeConfig = Launcher.gsonManager.gson.fromJson(reader, LaunchServerRuntimeConfig.class);
            }
        }

        LaunchServer.LaunchServerConfigManager launchServerConfigManager = new BasicLaunchServerConfigManager(configFile, runtimeConfigFile);
        LaunchServer server = new LaunchServerBuilder()
                .setDirectories(directories)
                .setEnv(env)
                .setCommandHandler(localCommandHandler)
                .setRuntimeConfig(runtimeConfig)
                .setConfig(config)
                .setModulesManager(modulesManager)
                .setLaunchServerConfigManager(launchServerConfigManager)
                .setCertificateManager(certificateManager)
                .build();
        List<String> allArgs = List.of(args);
        boolean isPrepareMode = prepareMode || allArgs.contains("--prepare");
        boolean isRunCommand = false;
        String runCommand = null;
        for(var e : allArgs) {
            if(e.equals("--run")) {
                isRunCommand = true;
                continue;
            }
            if(isRunCommand) {
                runCommand = e;
                isRunCommand = false;
            }
        }
        if(runCommand != null) {
            localCommandHandler.eval(runCommand, false);
        }
        if (!isPrepareMode) {
            server.run();
        } else {
            server.close();
        }
    }

    public static void initGson(LaunchServerModulesManager modulesManager) {
        Launcher.gsonManager = new LaunchServerGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
    }

    public static void registerAll() {
        AuthCoreProvider.registerProviders();
        PasswordVerifier.registerProviders();
        TextureProvider.registerProviders();
        Component.registerComponents();
        ProtectHandler.registerHandlers();
        WebSocketService.registerResponses();
        AuthRequest.registerProviders();
        GetAvailabilityAuthRequest.registerProviders();
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();
        MixProvider.registerProviders();
        ProfileProvider.registerProviders();
        UpdatesProvider.registerProviders();
    }

    private static void printExperimentalBranch() {
        try(Reader reader = IOHelper.newReader(IOHelper.getResourceURL("experimental-build.json"))) {
            ExperimentalBuild info = Launcher.gsonManager.configGson.fromJson(reader, ExperimentalBuild.class);
            if(info.features == null || info.features.isEmpty()) {
                return;
            }
            logger.warn("This is experimental build. Please do not use this in production");
            logger.warn("Experimental features: [{}]", String.join(",", info.features));
            for(var e : info.info) {
                logger.warn(e);
            }
        } catch (Throwable e) {
            logger.warn("Build information not found");
        }
    }

    record ExperimentalBuild(List<String> features, List<String> info) {

    }

    public static void generateConfigIfNotExists(Path configFile, CommandHandler commandHandler, LaunchServer.LaunchServerEnv env) throws IOException {
        if (IOHelper.isFile(configFile))
            return;

        // Create new config
        logger.info("Creating LaunchServer config");


        LaunchServerConfig newConfig = LaunchServerConfig.getDefault(env);
        // Set server address
        String address;
        if (env.equals(LaunchServer.LaunchServerEnv.TEST)) {
            address = "localhost";
            newConfig.setProjectName("test");
        } else {
            address = System.getenv("ADDRESS");
            if (address == null) {
                address = System.getProperty("launchserver.address", null);
            }
            if (address == null) {
                System.out.println("External launchServer address:port (default: localhost:9274): ");
                address = commandHandler.readLine();
            }
            String projectName = System.getenv("PROJECTNAME");
            if (projectName == null) {
                projectName = System.getProperty("launchserver.projectname", null);
            }
            if (projectName == null) {
                System.out.println("LaunchServer projectName: ");
                projectName = commandHandler.readLine();
            }
            newConfig.setProjectName(projectName);
        }
        if (address == null || address.isEmpty()) {
            logger.error("Address null. Using localhost:9274");
            address = "localhost:9274";
        }
        if (newConfig.projectName == null || newConfig.projectName.isEmpty()) {
            logger.error("ProjectName null. Using MineCraft");
            newConfig.projectName = "MineCraft";
        }
        int port = 9274;
        if(address.contains(":")) {
            String portString = address.substring(address.indexOf(':')+1);
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                logger.warn("Unknown port {}, using 9274", portString);
            }
        } else {
            logger.info("Address {} doesn't contains port (you want to use nginx?)", address);
        }
        newConfig.netty.address = "ws://" + address + "/api";
        newConfig.netty.downloadURL = "http://" + address + "/%dirname%/";
        newConfig.netty.launcherURL = "http://" + address + "/Launcher.jar";
        newConfig.netty.launcherEXEURL = "http://" + address + "/Launcher.exe";
        newConfig.netty.binds[0].port = port;

        // Write LaunchServer config
        logger.info("Writing LaunchServer config file");
        try (BufferedWriter writer = IOHelper.newWriter(configFile)) {
            Launcher.gsonManager.configGson.toJson(newConfig, writer);
        }
    }

    private static class BasicLaunchServerConfigManager implements LaunchServer.LaunchServerConfigManager {
        private final Path configFile;
        private final Path runtimeConfigFile;

        public BasicLaunchServerConfigManager(Path configFile, Path runtimeConfigFile) {
            this.configFile = configFile;
            this.runtimeConfigFile = runtimeConfigFile;
        }

        @Override
        public LaunchServerConfig readConfig() throws IOException {
            LaunchServerConfig config1;
            try (BufferedReader reader = IOHelper.newReader(configFile)) {
                config1 = Launcher.gsonManager.gson.fromJson(reader, LaunchServerConfig.class);
            }
            return config1;
        }

        @Override
        public LaunchServerRuntimeConfig readRuntimeConfig() throws IOException {
            LaunchServerRuntimeConfig config1;
            try (BufferedReader reader = IOHelper.newReader(runtimeConfigFile)) {
                config1 = Launcher.gsonManager.gson.fromJson(reader, LaunchServerRuntimeConfig.class);
            }
            return config1;
        }

        @Override
        public void writeConfig(LaunchServerConfig config) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (Writer writer = IOHelper.newWriter(output)) {
                if (Launcher.gsonManager.configGson != null) {
                    Launcher.gsonManager.configGson.toJson(config, writer);
                } else {
                    logger.error("Error writing LaunchServer config file. Gson is null");
                }
            }
            byte[] bytes = output.toByteArray();
            if(bytes.length > 0) {
                IOHelper.write(configFile, bytes);
            }
        }

        @Override
        public void writeRuntimeConfig(LaunchServerRuntimeConfig config) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (Writer writer = IOHelper.newWriter(output)) {
                if (Launcher.gsonManager.configGson != null) {
                    Launcher.gsonManager.configGson.toJson(config, writer);
                } else {
                    logger.error("Error writing LaunchServer runtime config file. Gson is null");
                }
            }
            byte[] bytes = output.toByteArray();
            if(bytes.length > 0) {
                IOHelper.write(runtimeConfigFile, bytes);
            }
        }
    }
}
