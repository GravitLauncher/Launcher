package pro.gravit.launchserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.base.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.base.modules.events.ClosePhase;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.RejectAuthCoreProvider;
import pro.gravit.launchserver.binary.EXELauncherBinary;
import pro.gravit.launchserver.binary.JARLauncherBinary;
import pro.gravit.launchserver.binary.LauncherBinary;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.helper.SignHelper;
import pro.gravit.launchserver.launchermodules.LauncherModuleLoader;
import pro.gravit.launchserver.manangers.*;
import pro.gravit.launchserver.manangers.hook.AuthHookManager;
import pro.gravit.launchserver.modules.events.*;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.SocketCommandServer;
import pro.gravit.launchserver.socket.handlers.NettyServerSocketHandler;
import pro.gravit.launchserver.socket.response.auth.ProfilesResponse;
import pro.gravit.launchserver.socket.response.auth.RestoreResponse;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main LaunchServer class. Contains links to all necessary objects
 * Not a singletron
 */
public final class LaunchServer implements Runnable, AutoCloseable, Reconfigurable {
    /**
     * Working folder path
     */
    public final Path dir;
    /**
     * Environment type (test / production)
     */
    public final LaunchServerEnv env;
    /**
     * The path to the folder with libraries for the launcher
     */
    public final Path launcherLibraries;
    /**
     * The path to the folder with compile-only libraries for the launcher
     */
    public final Path launcherLibrariesCompile;
    public final Path launcherPack;
    /**
     * The path to the folder with updates/webroot
     */
    @Deprecated
    public final Path updatesDir;

    // Constant paths
    /**
     * Save/Reload LaunchServer config
     */
    public final LaunchServerConfigManager launchServerConfigManager;
    /**
     * The path to the folder with profiles
     */
    public final Path tmpDir;
    public final Path modulesDir;
    public final Path launcherModulesDir;
    public final Path librariesDir;
    public final Path controlFile;
    /**
     * This object contains runtime configuration
     */
    public final LaunchServerRuntimeConfig runtime;
    /**
     * Pipeline for building JAR
     */
    public final JARLauncherBinary launcherBinary;
    /**
     * Pipeline for building EXE
     */
    public final LauncherBinary launcherEXEBinary;
    // Server config
    public final AuthHookManager authHookManager;
    public final LaunchServerModulesManager modulesManager;
    // Launcher binary
    public final MirrorManager mirrorManager;
    public final AuthManager authManager;
    public final ReconfigurableManager reconfigurableManager;
    public final ConfigManager configManager;
    public final FeaturesManager featuresManager;
    public final KeyAgreementManager keyAgreementManager;
    public final UpdatesManager updatesManager;
    // HWID ban + anti-brutforce
    public final CertificateManager certificateManager;
    // Server
    public final CommandHandler commandHandler;
    public final NettyServerSocketHandler nettyServerSocketHandler;
    public final SocketCommandServer socketCommandServer;
    public final ScheduledExecutorService service;
    public final AtomicBoolean started = new AtomicBoolean(false);
    public final LauncherModuleLoader launcherModuleLoader;
    private final Logger logger = LogManager.getLogger();
    public final int shardId;
    public LaunchServerConfig config;

    public LaunchServer(LaunchServerDirectories directories, LaunchServerEnv env, LaunchServerConfig config, LaunchServerRuntimeConfig runtimeConfig, LaunchServerConfigManager launchServerConfigManager, LaunchServerModulesManager modulesManager, KeyAgreementManager keyAgreementManager, CommandHandler commandHandler, CertificateManager certificateManager, int shardId) throws IOException {
        this.dir = directories.dir;
        this.tmpDir = directories.tmpDir;
        this.env = env;
        this.config = config;
        this.launchServerConfigManager = launchServerConfigManager;
        this.modulesManager = modulesManager;
        this.updatesDir = directories.updatesDir;
        this.keyAgreementManager = keyAgreementManager;
        this.commandHandler = commandHandler;
        this.runtime = runtimeConfig;
        this.certificateManager = certificateManager;
        this.service = Executors.newScheduledThreadPool(config.netty.performance.schedulerThread);
        launcherLibraries = directories.launcherLibrariesDir;
        launcherLibrariesCompile = directories.launcherLibrariesCompileDir;
        launcherPack = directories.launcherPackDir;
        modulesDir = directories.modules;
        launcherModulesDir = directories.launcherModules;
        librariesDir = directories.librariesDir;
        controlFile = directories.controlFile;
        this.shardId = shardId;
        if(!Files.isDirectory(launcherPack)) {
            Files.createDirectories(launcherPack);
        }

        config.setLaunchServer(this);

        modulesManager.invokeEvent(new NewLaunchServerInstanceEvent(this));

        // Print keypair fingerprints

        runtime.verify();
        config.verify();

        // build hooks, anti-brutforce and other
        mirrorManager = new MirrorManager();
        reconfigurableManager = new ReconfigurableManager();
        authHookManager = new AuthHookManager();
        configManager = new ConfigManager();
        featuresManager = new FeaturesManager(this);
        authManager = new AuthManager(this);
        updatesManager = new UpdatesManager(this);
        RestoreResponse.registerProviders(this);

        config.init(ReloadType.FULL);
        registerObject("launchServer", this);

        pro.gravit.launchserver.command.handler.CommandHandler.registerCommands(commandHandler, this);

        // init modules
        modulesManager.invokeEvent(new LaunchServerInitPhase(this));

        // Set launcher EXE binary
        launcherBinary = new JARLauncherBinary(this);
        launcherEXEBinary = binary();

        launcherBinary.init();
        launcherEXEBinary.init();
        syncLauncherBinaries();
        launcherModuleLoader = new LauncherModuleLoader(this);
        if (config.components != null) {
            logger.debug("Init components");
            config.components.forEach((k, v) -> {
                logger.debug("Init component {}", k);
                v.setComponentName(k);
                v.init(this);
            });
            logger.debug("Init components successful");
        }
        launcherModuleLoader.init();
        nettyServerSocketHandler = new NettyServerSocketHandler(this);
        socketCommandServer = new SocketCommandServer(commandHandler, controlFile);
        if(config.sign.checkCertificateExpired) {
            checkCertificateExpired();
            service.scheduleAtFixedRate(this::checkCertificateExpired, 24, 24, TimeUnit.HOURS);
        }
        // post init modules
        modulesManager.invokeEvent(new LaunchServerPostInitPhase(this));
    }

    public void reload(ReloadType type) throws Exception {
        config.close(type);
        Map<String, AuthProviderPair> pairs = null;
        if (type.equals(ReloadType.NO_AUTH)) {
            pairs = config.auth;
        }
        logger.info("Reading LaunchServer config file");
        config = launchServerConfigManager.readConfig();
        config.setLaunchServer(this);
        if (type.equals(ReloadType.NO_AUTH)) {
            config.auth = pairs;
        }
        config.verify();
        config.init(type);
        if (type.equals(ReloadType.FULL) && config.components != null) {
            logger.debug("Init components");
            config.components.forEach((k, v) -> {
                logger.debug("Init component {}", k);
                v.setComponentName(k);
                v.init(this);
            });
            logger.debug("Init components successful");
        }
        if(!type.equals(ReloadType.NO_AUTH)) {
            nettyServerSocketHandler.nettyServer.service.forEachActiveChannels((channel, wsHandler) -> {
                Client client = wsHandler.getClient();
                if(client.auth != null) {
                    client.auth = config.getAuthProviderPair(client.auth_id);
                }
            });
        }
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        SubCommand reload = new SubCommand("[type]", "reload launchserver config") {
            @Override
            public void invoke(String... args) throws Exception {
                if (args.length == 0) {
                    reload(ReloadType.FULL);
                    return;
                }
                switch (args[0]) {
                    case "full" -> reload(ReloadType.FULL);
                    case "no_components" -> reload(ReloadType.NO_COMPONENTS);
                    default -> reload(ReloadType.NO_AUTH);
                }
            }
        };
        commands.put("reload", reload);
        SubCommand save = new SubCommand("[]", "save launchserver config") {
            @Override
            public void invoke(String... args) throws Exception {
                launchServerConfigManager.writeConfig(config);
                launchServerConfigManager.writeRuntimeConfig(runtime);
                logger.info("LaunchServerConfig saved");
            }
        };
        commands.put("save", save);
        LaunchServer instance = this;
        SubCommand resetauth = new SubCommand("authId", "reset auth by id") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                AuthProviderPair pair = config.getAuthProviderPair(args[0]);
                if (pair == null) {
                    logger.error("Pair not found");
                    return;
                }
                pair.core.close();
                pair.core = new RejectAuthCoreProvider();
                pair.core.init(instance, pair);
            }
        };
        commands.put("resetauth", resetauth);
        return commands;
    }

    public void checkCertificateExpired() {
        if(!config.sign.enabled) {
            return;
        }
        try {
            KeyStore keyStore = SignHelper.getStore(Paths.get(config.sign.keyStore), config.sign.keyStorePass, config.sign.keyStoreType);
            Instant date = SignHelper.getCertificateExpired(keyStore, config.sign.keyAlias);
            if(date == null) {
                logger.debug("The certificate will expire at unlimited");
            } else if(date.minus(Duration.ofDays(30)).isBefore(Instant.now())) {
                logger.warn("The certificate will expire at {}", date.toString());
            } else {
                logger.debug("The certificate will expire at {}", date.toString());
            }
        } catch (Throwable e) {
            logger.error("Can't get certificate expire date", e);
        }
    }

    private LauncherBinary binary() {
        LaunchServerLauncherExeInit event = new LaunchServerLauncherExeInit(this, null);
        modulesManager.invokeEvent(event);
        if(event.binary != null) {
            return event.binary;
        }
        return new EXELauncherBinary(this);
    }

    public void buildLauncherBinaries() throws IOException {
        launcherBinary.build();
        launcherEXEBinary.build();
    }

    public void close() throws Exception {
        service.shutdownNow();
        logger.info("Close server socket");
        nettyServerSocketHandler.close();
        // Close handlers & providers
        config.close(ReloadType.FULL);
        modulesManager.invokeEvent(new ClosePhase());
        logger.info("Save LaunchServer runtime config");
        launchServerConfigManager.writeRuntimeConfig(runtime);
        // Print last message before death :(
        logger.info("LaunchServer stopped");
    }

    @Deprecated
    public Set<ClientProfile> getProfiles() {
        return config.profileProvider.getProfiles();
    }

    @Deprecated
    public void setProfiles(Set<ClientProfile> profilesList) {
        throw new UnsupportedOperationException();
    }

    public void rebindNettyServerSocket() {
        nettyServerSocketHandler.close();
        CommonHelper.newThread("Netty Server Socket Thread", false, nettyServerSocketHandler).start();
    }

    @Override
    public void run() {
        if (started.getAndSet(true))
            throw new IllegalStateException("LaunchServer has been already started");

        // Add shutdown hook, then start LaunchServer
        if (!this.env.equals(LaunchServerEnv.TEST)) {
            JVMHelper.RUNTIME.addShutdownHook(CommonHelper.newThread(null, false, () -> {
                try {
                    close();
                } catch (Exception e) {
                    logger.error("LaunchServer close error", e);
                }
            }));
            CommonHelper.newThread("Command Thread", true, commandHandler).start();
            CommonHelper.newThread("Socket Command Thread", true, socketCommandServer).start();
            // Sync updates dir
            CommonHelper.newThread("Profiles and updates sync", true, () -> {
                try {
                    // Sync profiles dir
                    syncProfilesDir();

                    // Sync updates dir
                    config.updatesProvider.syncInitially();


                    modulesManager.invokeEvent(new LaunchServerProfilesSyncEvent(this));
                } catch (IOException e) {
                    logger.error("Updates/Profiles not synced", e);
                }
            }).start();
        }
        if (config.netty != null)
            rebindNettyServerSocket();
        try {
            modulesManager.fullInitializedLaunchServer(this);
            modulesManager.invokeEvent(new LaunchServerFullInitEvent(this));
            logger.info("LaunchServer started");
        } catch (Throwable e) {
            logger.error("LaunchServer startup failed", e);
            JVMHelper.RUNTIME.exit(-1);
        }
    }

    public void syncLauncherBinaries() throws IOException {
        logger.info("Syncing launcher binaries");

        // Syncing launcher binary
        logger.info("Syncing launcher binary file");
        if (!launcherBinary.sync()) logger.warn("Missing launcher binary file");

        // Syncing launcher EXE binary
        logger.info("Syncing launcher EXE binary file");
        if (!launcherEXEBinary.sync())
            logger.warn("Missing launcher EXE binary file");

    }

    public void syncProfilesDir() throws IOException {
        logger.info("Syncing profiles dir");
        config.profileProvider.sync();
        if (config.netty.sendProfileUpdatesEvent) {
            sendUpdateProfilesEvent();
        }
    }

    private void sendUpdateProfilesEvent() {
        if (nettyServerSocketHandler == null || nettyServerSocketHandler.nettyServer == null || nettyServerSocketHandler.nettyServer.service == null) {
            return;
        }
        nettyServerSocketHandler.nettyServer.service.forEachActiveChannels((ch, handler) -> {
            Client client = handler.getClient();
            if (client == null || !client.isAuth) {
                return;
            }
            ProfilesRequestEvent event = new ProfilesRequestEvent(config.profileProvider.getProfiles(client));
            event.requestUUID = RequestEvent.eventUUID;
            handler.service.sendObject(ch, event);
        });
    }

    public void syncUpdatesDir(Collection<String> dirs) throws IOException {
        updatesManager.syncUpdatesDir(dirs);
    }

    public void registerObject(String name, Object object) {
        if (object instanceof Reconfigurable) {
            reconfigurableManager.registerReconfigurable(name, (Reconfigurable) object);
        }
    }

    public void unregisterObject(String name, Object object) {
        if (object instanceof Reconfigurable) {
            reconfigurableManager.unregisterReconfigurable(name);
        }
    }


    public enum ReloadType {
        NO_AUTH,
        NO_COMPONENTS,
        FULL
    }

    public enum LaunchServerEnv {
        TEST,
        DEV,
        DEBUG,
        PRODUCTION
    }

    public interface LaunchServerConfigManager {
        LaunchServerConfig readConfig() throws IOException;

        LaunchServerRuntimeConfig readRuntimeConfig() throws IOException;

        void writeConfig(LaunchServerConfig config) throws IOException;

        void writeRuntimeConfig(LaunchServerRuntimeConfig config) throws IOException;
    }

    public static class LaunchServerDirectories {
        public static final String UPDATES_NAME = "updates",
                TRUSTSTORE_NAME = "truststore", LAUNCHERLIBRARIES_NAME = "launcher-libraries",
                LAUNCHERLIBRARIESCOMPILE_NAME = "launcher-libraries-compile", LAUNCHERPACK_NAME = "launcher-pack", KEY_NAME = ".keys", MODULES = "modules", LAUNCHER_MODULES = "launcher-modules", LIBRARIES = "libraries", CONTROL_FILE = "control-file";
        public Path updatesDir;
        public Path librariesDir;
        public Path launcherLibrariesDir;
        public Path launcherLibrariesCompileDir;
        public Path launcherPackDir;
        public Path keyDirectory;
        public Path dir;
        public Path trustStore;
        public Path tmpDir;
        public Path modules;
        public Path launcherModules;
        public Path controlFile;

        public void collect() {
            if (updatesDir == null) updatesDir = getPath(UPDATES_NAME);
            if (trustStore == null) trustStore = getPath(TRUSTSTORE_NAME);
            if (launcherLibrariesDir == null) launcherLibrariesDir = getPath(LAUNCHERLIBRARIES_NAME);
            if (launcherLibrariesCompileDir == null)
                launcherLibrariesCompileDir = getPath(LAUNCHERLIBRARIESCOMPILE_NAME);
            if (launcherPackDir == null)
                launcherPackDir = getPath(LAUNCHERPACK_NAME);
            if (keyDirectory == null) keyDirectory = getPath(KEY_NAME);
            if (modules == null) modules = getPath(MODULES);
            if (launcherModules == null) launcherModules = getPath(LAUNCHER_MODULES);
            if (librariesDir == null) librariesDir = getPath(LIBRARIES);
            if (controlFile == null) controlFile = getPath(CONTROL_FILE);
            if (tmpDir == null)
                tmpDir = Paths.get(System.getProperty("java.io.tmpdir")).resolve("launchserver-%s".formatted(SecurityHelper.randomStringToken()));
        }

        private Path getPath(String dirName) {
            String property = System.getProperty("launchserver.dir." + dirName, null);
            if (property == null) return dir.resolve(dirName);
            else return Paths.get(property);
        }
    }
}
