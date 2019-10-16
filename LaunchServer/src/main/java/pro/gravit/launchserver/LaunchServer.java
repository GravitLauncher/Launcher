package pro.gravit.launchserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;

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
import pro.gravit.launchserver.binary.EXEL4JLauncherBinary;
import pro.gravit.launchserver.binary.EXELauncherBinary;
import pro.gravit.launchserver.binary.JARLauncherBinary;
import pro.gravit.launchserver.binary.LauncherBinary;
import pro.gravit.launchserver.binary.ProguardConf;
import pro.gravit.launchserver.binary.SimpleEXELauncherBinary;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.manangers.MirrorManager;
import pro.gravit.launchserver.manangers.ReconfigurableManager;
import pro.gravit.launchserver.manangers.SessionManager;
import pro.gravit.launchserver.manangers.hook.AuthHookManager;
import pro.gravit.launchserver.manangers.hook.BuildHookManager;
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

public final class LaunchServer implements Runnable, AutoCloseable, Reconfigurable {

    public enum ReloadType
    {
        NO_AUTH,
        NO_COMPONENTS,
        FULL
    }
    public enum LaunchServerEnv
    {
        TEST,
        DEV,
        DEBUG,
        PRODUCTION
    }
    public interface LaunchServerConfigManager
    {
        LaunchServerConfig readConfig() throws IOException;
        LaunchServerRuntimeConfig readRuntimeConfig() throws IOException;
        void writeConfig(LaunchServerConfig config) throws IOException;
        void writeRuntimeConfig(LaunchServerRuntimeConfig config) throws IOException;
    }

    public void reload(ReloadType type) throws Exception {
        config.close(type);
        AuthProviderPair[] pairs = null;
        if(type.equals(ReloadType.NO_AUTH))
        {
            pairs = config.auth;
        }
        LogHelper.info("Reading LaunchServer config file");
        config = launchServerConfigManager.readConfig();
        config.setLaunchServer(this);
        if(type.equals(ReloadType.NO_AUTH))
        {
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
                registerObject("component.".concat(k), v);
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
                if(args.length == 0)
                {
                    reload(ReloadType.FULL);
                    return;
                }
                switch (args[0])
                {
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
                        reload(ReloadType.FULL);;
                        break;
                }
            }
        };
        commands.put("reload", reload);
        return commands;
    }


    private final class ProfilesFileVisitor extends SimpleFileVisitor<Path> {
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

    // Constant paths

    public final Path dir;

    public final LaunchServerEnv env;

    public final Path launcherLibraries;

    public final Path launcherLibrariesCompile;

    public final Path caCertFile;

    public final Path caKeyFile;

    public final Path serverCertFile;

    public final Path serverKeyFile;

    public final Path updatesDir;

    public final LaunchServerConfigManager launchServerConfigManager;

    //public static LaunchServer server = null;

    public final Path profilesDir;
    // Server config

    public LaunchServerConfig config;
    public LaunchServerRuntimeConfig runtime;


    public final ECPublicKey publicKey;

    public final ECPrivateKey privateKey;
    // Launcher binary

    public final JARLauncherBinary launcherBinary;

    public Class<? extends LauncherBinary> launcherEXEBinaryClass;

    public final LauncherBinary launcherEXEBinary;
    // HWID ban + anti-brutforce

    public final SessionManager sessionManager;

    public final AuthHookManager authHookManager;
    // Server

    public final LaunchServerModulesManager modulesManager;

    public final MirrorManager mirrorManager;

    public final ReconfigurableManager reconfigurableManager;

    public final ConfigManager configManager;

    public final CertificateManager certificateManager;


    public final BuildHookManager buildHookManager;

    public final ProguardConf proguardConf;


    public final CommandHandler commandHandler;

    public final NettyServerSocketHandler nettyServerSocketHandler;

    private final AtomicBoolean started = new AtomicBoolean(false);

    // Updates and profiles
    private volatile List<ClientProfile> profilesList;
    public volatile Map<String, HashedDir> updatesDirMap;

    public final Timer taskPool;

    public static Class<? extends LauncherBinary> defaultLauncherEXEBinaryClass = null;

    public static class LaunchServerDirectories
    {
        public Path updatesDir;
        public Path profilesDir;
        public Path dir;
        public void collect()
        {
            if(updatesDir == null) updatesDir = dir.resolve("updates");
            if(profilesDir == null) profilesDir = dir.resolve("profiles");
        }
    }

    public LaunchServer(LaunchServerDirectories directories, LaunchServerEnv env, LaunchServerConfig config, LaunchServerRuntimeConfig runtimeConfig, LaunchServerConfigManager launchServerConfigManager, LaunchServerModulesManager modulesManager, ECPublicKey publicKey, ECPrivateKey privateKey, CommandHandler commandHandler) throws IOException, InvalidKeySpecException {
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
        taskPool = new Timer("Timered task worker thread", true);
        launcherLibraries = dir.resolve("launcher-libraries");
        launcherLibrariesCompile = dir.resolve("launcher-libraries-compile");

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
        if (config.components != null) {
            LogHelper.debug("PreInit components");
            config.components.forEach((k, v) -> {
                LogHelper.subDebug("PreInit component %s", k);
                v.preInit(this);
            });
            LogHelper.debug("PreInit components successful");
        }

        // build hooks, anti-brutforce and other
        buildHookManager = new BuildHookManager();
        proguardConf = new ProguardConf(this);
        sessionManager = new SessionManager();
        mirrorManager = new MirrorManager();
        reconfigurableManager = new ReconfigurableManager();
        authHookManager = new AuthHookManager();
        configManager = new ConfigManager();
        certificateManager = new CertificateManager();
        //Generate or set new Certificate API
        certificateManager.orgName = config.projectName;
        if(config.certificate != null && config.certificate.enabled)
        {
            if(IOHelper.isFile(caCertFile) && IOHelper.isFile(caKeyFile))
            {
                certificateManager.ca = certificateManager.readCertificate(caCertFile);
                certificateManager.caKey = certificateManager.readPrivateKey(caKeyFile);
            }
            else
            {
                try {
                    certificateManager.generateCA();
                    certificateManager.writeCertificate(caCertFile, certificateManager.ca);
                    certificateManager.writePrivateKey(caKeyFile, certificateManager.caKey);
                } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | OperatorCreationException e) {
                    LogHelper.error(e);
                }
            }
            if(IOHelper.isFile(serverCertFile) && IOHelper.isFile(serverKeyFile))
            {
                certificateManager.server = certificateManager.readCertificate(serverCertFile);
                certificateManager.serverKey = certificateManager.readPrivateKey(serverKeyFile);
            }
            else
            {
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

        // Sync updates dir
        if (!IOHelper.isDir(updatesDir))
            Files.createDirectory(updatesDir);
        syncUpdatesDir(null);

        // Sync profiles dir
        if (!IOHelper.isDir(profilesDir))
            Files.createDirectory(profilesDir);
        syncProfilesDir();

        if (config.netty != null)
            nettyServerSocketHandler = new NettyServerSocketHandler(this);
        else
            nettyServerSocketHandler = null;
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

    private LauncherBinary binary() {
        if (launcherEXEBinaryClass != null) {
            try {
                return launcherEXEBinaryClass.getConstructor(LaunchServer.class).newInstance(this);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                LogHelper.error(e);
            }
        }
        if(config.launch4j.alternative != null)
        {
            switch (config.launch4j.alternative) {
                case "simple":
                    return new SimpleEXELauncherBinary(this);
                case "no":
                    //None
                    break;
                default:
                    LogHelper.warning("Alternative %s not found", config.launch4j.alternative);
                    break;
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
        modulesManager.fullInitializedLaunchServer(this);
        modulesManager.invokeEvent(new LaunchServerFullInitEvent(this));
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
                    if (!IOHelper.isFile(updateDir) && Arrays.asList(".jar", ".exe", ".hash").stream().noneMatch(e -> updateDir.toString().endsWith(e)))
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
}
