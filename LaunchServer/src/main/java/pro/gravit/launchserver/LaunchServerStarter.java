package pro.gravit.launchserver;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherTrustManager;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.protect.hwid.HWIDProvider;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.session.SessionStorage;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.dao.provider.DaoProvider;
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
import pro.gravit.utils.helper.SecurityHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public class LaunchServerStarter {
    public static final boolean allowUnsigned = Boolean.getBoolean("launchserver.allowUnsigned");
    public static final boolean inDocker = Boolean.getBoolean("launchserver.dockered");

    public static void main(String[] args) throws Exception {
        JVMHelper.checkStackTrace(LaunchServerStarter.class);
        JVMHelper.verifySystemProperties(LaunchServer.class, true);
        LogHelper.addOutput(IOHelper.WORKING_DIR.resolve("LaunchServer.log"));
        LogHelper.printVersion("LaunchServer");
        LogHelper.printLicense("LaunchServer");
        if (!StarterAgent.isAgentStarted()) {
            LogHelper.error("StarterAgent is not started!");
            LogHelper.error("You should add to JVM options this option: `-javaagent:LaunchServer.jar`");
        }
        Path dir = IOHelper.WORKING_DIR;
        Path configFile, runtimeConfigFile;
        Path publicKeyFile = dir.resolve("public.key");
        Path privateKeyFile = dir.resolve("private.key");
        ECPublicKey publicKey;
        ECPrivateKey privateKey;
        try {
            Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider(new BouncyCastleProvider());
        } catch (ClassNotFoundException ex) {
            LogHelper.error("Library BouncyCastle not found! Is directory 'libraries' empty?");
            return;
        }
        CertificateManager certificateManager = new CertificateManager();
        try {
            certificateManager.readTrustStore(dir.resolve("truststore"));
        } catch (CertificateException e) {
            throw new IOException(e);
        }
        {
            //LauncherTrustManager.CheckMode mode = (Version.RELEASE == Version.Type.LTS || Version.RELEASE == Version.Type.STABLE) ?
            //        (allowUnsigned ? LauncherTrustManager.CheckMode.WARN_IN_NOT_SIGNED : LauncherTrustManager.CheckMode.EXCEPTION_IN_NOT_SIGNED) :
            //        (allowUnsigned ? LauncherTrustManager.CheckMode.NONE_IN_NOT_SIGNED : LauncherTrustManager.CheckMode.WARN_IN_NOT_SIGNED);
            LauncherTrustManager.CheckClassResult result = certificateManager.checkClass(LaunchServer.class);
            if(result.type == LauncherTrustManager.CheckClassResultType.SUCCESS) {
                LogHelper.info("LaunchServer signed by %s", result.endCertificate.getSubjectDN().getName());
            }
            else if(result.type == LauncherTrustManager.CheckClassResultType.NOT_SIGNED) {
                // None
            }
            else {
                if(result.exception != null) {
                    LogHelper.error(result.exception);
                }
                LogHelper.warning("LaunchServer signed incorrectly. Status: %s", result.type.name());
            }
        }

        LaunchServerRuntimeConfig runtimeConfig;
        LaunchServerConfig config;
        LaunchServer.LaunchServerEnv env = LaunchServer.LaunchServerEnv.PRODUCTION;
        LaunchServerModulesManager modulesManager = new LaunchServerModulesManager(dir.resolve("modules"), dir.resolve("config"), certificateManager.trustManager);
        modulesManager.autoload();
        modulesManager.initModules(null);
        registerAll();
        initGson(modulesManager);
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
            LogHelper.info("JLine2 terminal enabled");
        } catch (ClassNotFoundException ignored) {
            localCommandHandler = new StdCommandHandler(true);
            LogHelper.warning("JLine2 isn't in classpath, using std");
        }
        if (IOHelper.isFile(publicKeyFile) && IOHelper.isFile(privateKeyFile)) {
            LogHelper.info("Reading EC keypair");
            publicKey = SecurityHelper.toPublicECKey(IOHelper.read(publicKeyFile));
            privateKey = SecurityHelper.toPrivateECKey(IOHelper.read(privateKeyFile));
        } else {
            LogHelper.info("Generating EC keypair");
            KeyPair pair = SecurityHelper.genECKeyPair(new SecureRandom());
            publicKey = (ECPublicKey) pair.getPublic();
            privateKey = (ECPrivateKey) pair.getPrivate();

            // Write key pair list
            LogHelper.info("Writing EC keypair list");
            IOHelper.write(publicKeyFile, publicKey.getEncoded());
            IOHelper.write(privateKeyFile, privateKey.getEncoded());
        }
        modulesManager.invokeEvent(new PreConfigPhase());
        generateConfigIfNotExists(configFile, localCommandHandler, env);
        LogHelper.info("Reading LaunchServer config file");
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = Launcher.gsonManager.gson.fromJson(reader, LaunchServerConfig.class);
        }
        if (!Files.exists(runtimeConfigFile)) {
            LogHelper.info("Reset LaunchServer runtime config file");
            runtimeConfig = new LaunchServerRuntimeConfig();
            runtimeConfig.reset();
        } else {
            LogHelper.info("Reading LaunchServer runtime config file");
            try (BufferedReader reader = IOHelper.newReader(runtimeConfigFile)) {
                runtimeConfig = Launcher.gsonManager.gson.fromJson(reader, LaunchServerRuntimeConfig.class);
            }
        }

        LaunchServer.LaunchServerConfigManager launchServerConfigManager = new LaunchServer.LaunchServerConfigManager() {
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
                try (Writer writer = IOHelper.newWriter(configFile)) {
                    if (Launcher.gsonManager.configGson != null) {
                        Launcher.gsonManager.configGson.toJson(config, writer);
                    } else {
                        LogHelper.error("Error writing LaunchServer runtime config file. Gson is null");
                    }
                }
            }

            @Override
            public void writeRuntimeConfig(LaunchServerRuntimeConfig config) throws IOException {
                try (Writer writer = IOHelper.newWriter(runtimeConfigFile)) {
                    if (Launcher.gsonManager.configGson != null) {
                        Launcher.gsonManager.configGson.toJson(config, writer);
                    } else {
                        LogHelper.error("Error writing LaunchServer runtime config file. Gson is null");
                    }
                }
            }
        };
        LaunchServer.LaunchServerDirectories directories = new LaunchServer.LaunchServerDirectories();
        directories.dir = dir;
        if (inDocker) {
            Path parentLibraries = StarterAgent.libraries.toAbsolutePath().normalize().getParent();
            directories.launcherLibrariesCompileDir = parentLibraries.resolve(LaunchServer.LaunchServerDirectories.LAUNCHERLIBRARIESCOMPILE_NAME);
            directories.launcherLibrariesDir = parentLibraries.resolve(LaunchServer.LaunchServerDirectories.LAUNCHERLIBRARIES_NAME);
        }
        LaunchServer server = new LaunchServerBuilder()
                .setDirectories(directories)
                .setEnv(env)
                .setCommandHandler(localCommandHandler)
                .setPrivateKey(privateKey)
                .setPublicKey(publicKey)
                .setRuntimeConfig(runtimeConfig)
                .setConfig(config)
                .setModulesManager(modulesManager)
                .setLaunchServerConfigManager(launchServerConfigManager)
                .setCertificateManager(certificateManager)
                .build();
        server.run();
    }

    public static void initGson(LaunchServerModulesManager modulesManager) {
        Launcher.gsonManager = new LaunchServerGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
    }

    public static void registerAll() {

        AuthHandler.registerHandlers();
        AuthProvider.registerProviders();
        TextureProvider.registerProviders();
        Component.registerComponents();
        ProtectHandler.registerHandlers();
        WebSocketService.registerResponses();
        DaoProvider.registerProviders();
        AuthRequest.registerProviders();
        HWIDProvider.registerProviders();
        OptionalAction.registerProviders();
        SessionStorage.registerProviders();
    }

    public static void generateConfigIfNotExists(Path configFile, CommandHandler commandHandler, LaunchServer.LaunchServerEnv env) throws IOException {
        if (IOHelper.isFile(configFile))
            return;

        // Create new config
        LogHelper.info("Creating LaunchServer config");


        LaunchServerConfig newConfig = LaunchServerConfig.getDefault(env);
        // Set server address
        String address;
        if (env.equals(LaunchServer.LaunchServerEnv.TEST)) {
            address = "localhost";
            newConfig.setProjectName("test");
        } else {
            System.out.println("LaunchServer address(default: localhost): ");
            address = commandHandler.readLine();
            System.out.println("LaunchServer projectName: ");
            newConfig.setProjectName(commandHandler.readLine());
        }
        if (address == null || address.isEmpty()) {
            LogHelper.error("Address null. Using localhost");
            address = "localhost";
        }
        if (newConfig.projectName == null || newConfig.projectName.isEmpty()) {
            LogHelper.error("ProjectName null. Using MineCraft");
            newConfig.projectName = "MineCraft";
        }

        newConfig.netty.address = "ws://" + address + ":9274/api";
        newConfig.netty.downloadURL = "http://" + address + ":9274/%dirname%/";
        newConfig.netty.launcherURL = "http://" + address + ":9274/Launcher.jar";
        newConfig.netty.launcherEXEURL = "http://" + address + ":9274/Launcher.exe";

        // Write LaunchServer config
        LogHelper.info("Writing LaunchServer config file");
        try (BufferedWriter writer = IOHelper.newWriter(configFile)) {
            Launcher.gsonManager.configGson.toJson(newConfig, writer);
        }
    }
}
