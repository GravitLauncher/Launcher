package pro.gravit.launchserver;

import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.OperatorCreationException;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.managers.ConfigManager;
import pro.gravit.launcher.managers.GarbageManager;
import pro.gravit.launcher.modules.events.ClosePhase;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.session.MemorySessionStorage;
import pro.gravit.launchserver.binary.*;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.launchermodules.LauncherModuleLoader;
import pro.gravit.launchserver.manangers.*;
import pro.gravit.launchserver.manangers.hook.AuthHookManager;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.launchserver.modules.events.NewLaunchServerInstanceEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.launchserver.socket.handlers.NettyServerSocketHandler;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public final class LaunchServer implements Runnable, AutoCloseable, Reconfigurable {

    public static final Class<? extends LauncherBinary> defaultLauncherEXEBinaryClass = null;
    public final Path dir;
    public final LaunchServerEnv env;
    public final Path launcherLibraries;
    public final Path launcherLibrariesCompile;
    public final Path caCertFile;

    // Constant paths
    public final Path caKeyFile;
    public final Path serverCertFile;
    public final Path serverKeyFile;
    public final Path updatesDir;
    public final LaunchServerConfigManager launchServerConfigManager;
    public final Path profilesDir;
    public final LaunchServerRuntimeConfig runtime;
    public final ECPublicKey publicKey;
    public final ECPrivateKey privateKey;
    public final JARLauncherBinary launcherBinary;

    //public static LaunchServer server = null;
    public final Class<? extends LauncherBinary> launcherEXEBinaryClass;
    // Server config
    public final LauncherBinary launcherEXEBinary;
    public final SessionManager sessionManager;
    public final AuthHookManager authHookManager;
    public final LaunchServerModulesManager modulesManager;
    // Launcher binary
    public final MirrorManager mirrorManager;
    public final ReconfigurableManager reconfigurableManager;
    public final ConfigManager configManager;
    public final PingServerManager pingServerManager;
    public final FeaturesManager featuresManager;
    // HWID ban + anti-brutforce
    public final CertificateManager certificateManager;
    public final ProguardConf proguardConf;
    // Server
    public final CommandHandler commandHandler;
    public final NettyServerSocketHandler nettyServerSocketHandler;
    public final Timer taskPool;
    public final AtomicBoolean started = new AtomicBoolean(false);
    public final LauncherModuleLoader launcherModuleLoader;
    public LaunchServerConfig config;
    public volatile Map<String, HashedDir> updatesDirMap;
    // Updates and profiles
    private volatile List<ClientProfile> profilesList;

    public LaunchServer(LaunchServerDirectories directories, LaunchServerEnv env, LaunchServerConfig config, LaunchServerRuntimeConfig runtimeConfig, LaunchServerConfigManager launchServerConfigManager, LaunchServerModulesManager modulesManager, ECPublicKey publicKey, ECPrivateKey privateKey, CommandHandler commandHandler, CertificateManager certificateManager) throws IOException {
        this.dir = directories.dir;
        this.env = env;
        this.config = config;
        this.launchServerConfigManager = launchServerConfigManager;
        this.modulesManager = modulesManager;
        this.profilesDir = directories.profilesDir;
        this.updatesDir = directories.updatesDir;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.commandHandler = commandHandler;
        this.runtime = runtimeConfig;
        this.certificateManager = certificateManager;
        taskPool = new Timer("Timered task worker thread", true);
        launcherLibraries = directories.launcherLibrariesDir;
        launcherLibrariesCompile = directories.launcherLibrariesCompileDir;

        config.setLaunchServer(this);

        caCertFile = dir.resolve("ca.crt");
        caKeyFile = dir.resolve("ca.key");

        serverCertFile = dir.resolve("server.crt");
        serverKeyFile = dir.resolve("server.key");

        modulesManager.invokeEvent(new NewLaunchServerInstanceEvent(this));

        // Print keypair fingerprints

        // Load class bindings.
        launcherEXEBinaryClass = defaultLauncherEXEBinaryClass;

        runtime.verify();
        config.verify();
        if(config.sessions == null) config.sessions = new MemorySessionStorage();
        if (config.components != null) {
            LogHelper.debug("PreInit components");
            config.components.forEach((k, v) -> {
                LogHelper.subDebug("PreInit component %s", k);
                v.preInit(this);
            });
            LogHelper.debug("PreInit components successful");
        }

        // build hooks, anti-brutforce and other
        proguardConf = new ProguardConf(this);
        sessionManager = new SessionManager(this);
        mirrorManager = new MirrorManager();
        reconfigurableManager = new ReconfigurableManager();
        authHookManager = new AuthHookManager();
        configManager = new ConfigManager();
        pingServerManager = new PingServerManager(this);
        featuresManager = new FeaturesManager(this);
        //Generate or set new Certificate API
        certificateManager.orgName = config.projectName;
        /*
        if (false) {
            if (IOHelper.isFile(caCertFile) && IOHelper.isFile(caKeyFile)) {
                certificateManager.ca = certificateManager.readCertificate(caCertFile);
                certificateManager.caKey = certificateManager.readPrivateKey(caKeyFile);
            } else {
                try {
                    certificateManager.generateCA();
                    certificateManager.writeCertificate(caCertFile, certificateManager.ca);
                    certificateManager.writePrivateKey(caKeyFile, certificateManager.caKey);
                } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | OperatorCreationException e) {
                    LogHelper.error(e);
                }
            }
            if (IOHelper.isFile(serverCertFile) && IOHelper.isFile(serverKeyFile)) {
                certificateManager.server = certificateManager.readCertificate(serverCertFile);
                certificateManager.serverKey = certificateManager.readPrivateKey(serverKeyFile);
            } else {
                try {
                    KeyPair pair = certificateManager.generateKeyPair();
                    certificateManager.server = certificateManager.generateCertificate(config.projectName.concat(" Server"), pair.getPublic());
                    certificateManager.serverKey = PrivateKeyFactory.createKey(pair.getPrivate().getEncoded());
                    certificateManager.writePrivateKey(serverKeyFile, pair.getPrivate());
                    certificateManager.writeCertificate(serverCertFile, certificateManager.server);
                } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | OperatorCreationException e) {
                    LogHelper.error(e);
                }
            }
        }
        */
        config.init(ReloadType.FULL);
        registerObject("launchServer", this);
        GarbageManager.registerNeedGC(sessionManager);

        pro.gravit.launchserver.command.handler.CommandHandler.registerCommands(commandHandler, this);

        // init modules
        modulesManager.invokeEvent(new LaunchServerInitPhase(this));
        if (config.components != null) {
            LogHelper.debug("Init components");
            config.components.forEach((k, v) -> {
                LogHelper.subDebug("Init component %s", k);
                v.init(this);
            });
            LogHelper.debug("Init components successful");
        }

        // Set launcher EXE binary
        launcherBinary = new JARLauncherBinary(this);
        launcherEXEBinary = binary();

        launcherBinary.init();
        launcherEXEBinary.init();
        syncLauncherBinaries();
        launcherModuleLoader = new LauncherModuleLoader(this);
        // Sync updates dir
        if (!IOHelper.isDir(updatesDir))
            Files.createDirectory(updatesDir);
        syncUpdatesDir(null);

        // Sync profiles dir
        if (!IOHelper.isDir(profilesDir))
            Files.createDirectory(profilesDir);
        syncProfilesDir();
        launcherModuleLoader.init();
        nettyServerSocketHandler = new NettyServerSocketHandler(this);
        // post init modules
        modulesManager.invokeEvent(new LaunchServerPostInitPhase(this));
        if (config.components != null) {
            LogHelper.debug("PostInit components");
            config.components.forEach((k, v) -> {
                LogHelper.subDebug("PostInit component %s", k);
                v.postInit(this);
            });
            LogHelper.debug("PostInit components successful");
        }
    }

    public void reload(ReloadType type) throws Exception {
        config.close(type);
        Map<String, AuthProviderPair> pairs = null;
        if (type.equals(ReloadType.NO_AUTH)) {
            pairs = config.auth;
        }
        LogHelper.info("Reading LaunchServer config file");
        config = launchServerConfigManager.readConfig();
        config.setLaunchServer(this);
        if (type.equals(ReloadType.NO_AUTH)) {
            config.auth = pairs;
        }
        config.verify();
        config.init(type);
        if (type.equals(ReloadType.FULL) && config.components != null) {
            LogHelper.debug("PreInit components");
            config.components.forEach((k, v) -> {
                LogHelper.subDebug("PreInit component %s", k);
                v.preInit(this);
            });
            LogHelper.debug("PreInit components successful");
            LogHelper.debug("Init components");
            config.components.forEach((k, v) -> {
                LogHelper.subDebug("Init component %s", k);
                v.init(this);
            });
            LogHelper.debug("Init components successful");
            LogHelper.debug("PostInit components");
            config.components.forEach((k, v) -> {
                LogHelper.subDebug("PostInit component %s", k);
                v.postInit(this);
            });
            LogHelper.debug("PostInit components successful");
        }

    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        SubCommand reload = new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                if (args.length == 0) {
                    reload(ReloadType.FULL);
                    return;
                }
                switch (args[0]) {
                    case "full":
                        reload(ReloadType.FULL);
                        break;
                    case "no_auth":
                        reload(ReloadType.NO_AUTH);
                        break;
                    case "no_components":
                        reload(ReloadType.NO_COMPONENTS);
                        break;
                    default:
                        reload(ReloadType.FULL);
                        break;
                }
            }
        };
        commands.put("reload", reload);
        SubCommand save = new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                launchServerConfigManager.writeConfig(config);
                launchServerConfigManager.writeRuntimeConfig(runtime);
                LogHelper.info("LaunchServerConfig saved");
            }
        };
        commands.put("save", save);
        return commands;
    }

    private LauncherBinary binary() {
        if (launcherEXEBinaryClass != null) {
            try {
                return (LauncherBinary) MethodHandles.publicLookup().findConstructor(launcherEXEBinaryClass, MethodType.methodType(void.class, LaunchServer.class)).invoke(this);
            } catch (Throwable e) {
                LogHelper.error(e);
            }
        }
        try {
            Class.forName("net.sf.launch4j.Builder");
            if (config.launch4j.enabled) return new EXEL4JLauncherBinary(this);
        } catch (ClassNotFoundException ignored) {
            LogHelper.warning("Launch4J isn't in classpath.");
        }
        return new EXELauncherBinary(this);
    }

    public void buildLauncherBinaries() throws IOException {
        launcherBinary.build();
        launcherEXEBinary.build();
    }

    public void close() throws Exception {
        LogHelper.info("Close server socket");
        nettyServerSocketHandler.close();
        // Close handlers & providers
        config.close(ReloadType.FULL);
        modulesManager.invokeEvent(new ClosePhase());
        LogHelper.info("Save LaunchServer runtime config");
        launchServerConfigManager.writeRuntimeConfig(runtime);
        // Print last message before death :(
        LogHelper.info("LaunchServer stopped");
    }

    public List<ClientProfile> getProfiles() {
        return profilesList;
    }

    public void setProfiles(List<ClientProfile> profilesList) {
        this.profilesList = Collections.unmodifiableList(profilesList);
    }

    public HashedDir getUpdateDir(String name) {
        return updatesDirMap.get(name);
    }

    public Set<Entry<String, HashedDir>> getUpdateDirs() {
        return updatesDirMap.entrySet();
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
                    LogHelper.error(e);
                }
            }));
            CommonHelper.newThread("Command Thread", true, commandHandler).start();
        }
        if (config.netty != null)
            rebindNettyServerSocket();
        try {
            modulesManager.fullInitializedLaunchServer(this);
            modulesManager.invokeEvent(new LaunchServerFullInitEvent(this));
            LogHelper.info("LaunchServer started");
        } catch (Throwable e) {
            LogHelper.error(e);
            JVMHelper.RUNTIME.exit(-1);
        }
    }

    public void syncLauncherBinaries() throws IOException {
        LogHelper.info("Syncing launcher binaries");

        // Syncing launcher binary
        LogHelper.info("Syncing launcher binary file");
        if (!launcherBinary.sync()) LogHelper.warning("Missing launcher binary file");

        // Syncing launcher EXE binary
        LogHelper.info("Syncing launcher EXE binary file");
        if (!launcherEXEBinary.sync() && config.launch4j.enabled)
            LogHelper.warning("Missing launcher EXE binary file");

    }

    public void syncProfilesDir() throws IOException {
        LogHelper.info("Syncing profiles dir");
        List<ClientProfile> newProfies = new LinkedList<>();
        IOHelper.walk(profilesDir, new ProfilesFileVisitor(newProfies), false);

        // Sort and set new profiles
        newProfies.sort(Comparator.comparing(a -> a));
        profilesList = Collections.unmodifiableList(newProfies);
        if (pingServerManager != null)
            pingServerManager.syncServers();
    }

    public void syncUpdatesDir(Collection<String> dirs) throws IOException {
        LogHelper.info("Syncing updates dir");
        Map<String, HashedDir> newUpdatesDirMap = new HashMap<>(16);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(updatesDir)) {
            for (final Path updateDir : dirStream) {
                if (Files.isHidden(updateDir))
                    continue; // Skip hidden

                // Resolve name and verify is dir
                String name = IOHelper.getFileName(updateDir);
                if (!IOHelper.isDir(updateDir)) {
                    if (!IOHelper.isFile(updateDir) && Stream.of(".jar", ".exe", ".hash").noneMatch(e -> updateDir.toString().endsWith(e)))
                        LogHelper.warning("Not update dir: '%s'", name);
                    continue;
                }

                // Add from previous map (it's guaranteed to be non-null)
                if (dirs != null && !dirs.contains(name)) {
                    HashedDir hdir = updatesDirMap.get(name);
                    if (hdir != null) {
                        newUpdatesDirMap.put(name, hdir);
                        continue;
                    }
                }

                // Sync and sign update dir
                LogHelper.info("Syncing '%s' update dir", name);
                HashedDir updateHDir = new HashedDir(updateDir, null, true, true);
                newUpdatesDirMap.put(name, updateHDir);
            }
        }
        updatesDirMap = Collections.unmodifiableMap(newUpdatesDirMap);
    }

    public void restart() {
        ProcessBuilder builder = new ProcessBuilder();
        if (config.startScript != null) builder.command(Collections.singletonList(config.startScript));
        else throw new IllegalArgumentException("Please create start script and link it as startScript in config.");
        builder.directory(this.dir.toFile());
        builder.inheritIO();
        builder.redirectErrorStream(true);
        builder.redirectOutput(Redirect.PIPE);
        try {
            builder.start();
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public void registerObject(String name, Object object) {
        if (object instanceof Reconfigurable) {
            reconfigurableManager.registerReconfigurable(name, (Reconfigurable) object);
        }
        if (object instanceof NeedGarbageCollection) {
            GarbageManager.registerNeedGC((NeedGarbageCollection) object);
        }
    }

    public void unregisterObject(String name, Object object) {
        if (object instanceof Reconfigurable) {
            reconfigurableManager.unregisterReconfigurable(name);
        }
        if (object instanceof NeedGarbageCollection) {
            GarbageManager.unregisterNeedGC((NeedGarbageCollection) object);
        }
    }

    public void fullyRestart() {
        restart();
        JVMHelper.RUNTIME.exit(0);
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

    private static final class ProfilesFileVisitor extends SimpleFileVisitor<Path> {
        private final Collection<ClientProfile> result;

        private ProfilesFileVisitor(Collection<ClientProfile> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            LogHelper.info("Syncing '%s' profile", IOHelper.getFileName(file));

            // Read profile
            ClientProfile profile;
            try (BufferedReader reader = IOHelper.newReader(file)) {
                profile = Launcher.gsonManager.gson.fromJson(reader, ClientProfile.class);
            }
            profile.verify();

            // Add SIGNED profile to result list
            result.add(profile);
            return super.visitFile(file, attrs);
        }
    }

    public static class LaunchServerDirectories {
        public static final String UPDATES_NAME = "updates", PROFILES_NAME = "profiles",
                TRUSTSTORE_NAME = "truststore", LAUNCHERLIBRARIES_NAME = "launcher-libraries",
                LAUNCHERLIBRARIESCOMPILE_NAME = "launcher-libraries-compile";
        public Path updatesDir;
        public Path profilesDir;
        public Path launcherLibrariesDir;
        public Path launcherLibrariesCompileDir;
        public Path dir;
        public Path trustStore;

        public void collect() {
            if (updatesDir == null) updatesDir = dir.resolve(UPDATES_NAME);
            if (profilesDir == null) profilesDir = dir.resolve(PROFILES_NAME);
            if (trustStore == null) trustStore = dir.resolve(TRUSTSTORE_NAME);
            if (launcherLibrariesDir == null) launcherLibrariesDir = dir.resolve(LAUNCHERLIBRARIES_NAME);
            if (launcherLibrariesCompileDir == null)
                launcherLibrariesCompileDir = dir.resolve(LAUNCHERLIBRARIESCOMPILE_NAME);
        }
    }
}
