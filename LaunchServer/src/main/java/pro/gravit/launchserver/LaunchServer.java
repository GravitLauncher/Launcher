package pro.gravit.launchserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
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

import io.netty.channel.epoll.Epoll;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.OperatorCreationException;

import io.netty.handler.logging.LogLevel;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.NeedGarbageCollection;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.hwid.HWIDProvider;
import pro.gravit.launcher.managers.ConfigManager;
import pro.gravit.launcher.managers.GarbageManager;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.handler.MemoryAuthHandler;
import pro.gravit.launchserver.auth.hwid.AcceptHWIDHandler;
import pro.gravit.launchserver.auth.hwid.HWIDHandler;
import pro.gravit.launchserver.auth.permissions.DefaultPermissionsHandler;
import pro.gravit.launchserver.auth.permissions.JsonFilePermissionsHandler;
import pro.gravit.launchserver.auth.permissions.PermissionsHandler;
import pro.gravit.launchserver.auth.protect.ProtectHandler;
import pro.gravit.launchserver.auth.protect.StdProtectHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.RejectAuthProvider;
import pro.gravit.launchserver.auth.texture.RequestTextureProvider;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.binary.EXEL4JLauncherBinary;
import pro.gravit.launchserver.binary.EXELauncherBinary;
import pro.gravit.launchserver.binary.JARLauncherBinary;
import pro.gravit.launchserver.binary.LauncherBinary;
import pro.gravit.launchserver.binary.ProguardConf;
import pro.gravit.launchserver.binary.SimpleEXELauncherBinary;
import pro.gravit.launchserver.components.AuthLimiterComponent;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.components.RegLimiterComponent;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LaunchServerRuntimeConfig;
import pro.gravit.launchserver.dao.provider.DaoProvider;
import pro.gravit.launchserver.manangers.CertificateManager;
import pro.gravit.launchserver.manangers.LaunchServerGsonManager;
import pro.gravit.launchserver.manangers.MirrorManager;
import pro.gravit.launchserver.manangers.ModulesManager;
import pro.gravit.launchserver.manangers.ReconfigurableManager;
import pro.gravit.launchserver.manangers.SessionManager;
import pro.gravit.launchserver.manangers.hook.AuthHookManager;
import pro.gravit.launchserver.manangers.hook.BuildHookManager;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.launchserver.socket.handlers.NettyServerSocketHandler;
import pro.gravit.utils.Version;
import pro.gravit.utils.command.*;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

public final class LaunchServer implements Runnable, AutoCloseable, Reconfigurable {

    public enum ReloadType
    {
        NO_AUTH,
        NO_COMPONENTS,
        FULL
    }

    public void reload(ReloadType type) throws Exception {
        config.close(type);
        AuthProviderPair[] pairs = null;
        if(type.equals(ReloadType.NO_AUTH))
        {
            pairs = config.auth;
        }
        LogHelper.info("Reading LaunchServer config file");
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = Launcher.gsonManager.gson.fromJson(reader, LaunchServerConfig.class);
        }
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

    public static void main(String... args) throws Throwable {
        JVMHelper.checkStackTrace(LaunchServer.class);
        JVMHelper.verifySystemProperties(LaunchServer.class, true);
        LogHelper.addOutput(IOHelper.WORKING_DIR.resolve("LaunchServer.log"));
        LogHelper.printVersion("LaunchServer");
        LogHelper.printLicense("LaunchServer");
        if (!StarterAgent.isAgentStarted()) {
            LogHelper.error("StarterAgent is not started!");
            LogHelper.error("You should add to JVM options this option: `-javaagent:LaunchServer.jar`");
        }

        // Start LaunchServer
        long startTime = System.currentTimeMillis();
        try {
            @SuppressWarnings("resource")
            LaunchServer launchserver = new LaunchServer(IOHelper.WORKING_DIR, false, args);
            if (args.length == 0) launchserver.run();
            else { //Обработка команды
                launchserver.commandHandler.eval(args, false);
            }
        } catch (Throwable exc) {
            LogHelper.error(exc);
            return;
        }
        long endTime = System.currentTimeMillis();
        LogHelper.debug("LaunchServer started in %dms", endTime - startTime);
    }

    // Constant paths

    public final Path dir;

    public final boolean testEnv;

    public final Path launcherLibraries;

    public final Path launcherLibrariesCompile;

    public final List<String> args;

    public final Path configFile;
    public final Path runtimeConfigFile;

    public final Path publicKeyFile;

    public final Path privateKeyFile;

    public final Path caCertFile;

    public final Path caKeyFile;

    public final Path serverCertFile;

    public final Path serverKeyFile;

    public final Path updatesDir;

    //public static LaunchServer server = null;

    public final Path profilesDir;
    // Server config

    public LaunchServerConfig config;
    public LaunchServerRuntimeConfig runtime;


    public final RSAPublicKey publicKey;

    public final RSAPrivateKey privateKey;
    // Launcher binary

    public final JARLauncherBinary launcherBinary;

    public Class<? extends LauncherBinary> launcherEXEBinaryClass;

    public final LauncherBinary launcherEXEBinary;
    // HWID ban + anti-brutforce

    public final SessionManager sessionManager;

    public final AuthHookManager authHookManager;
    // Server

    public final ModulesManager modulesManager;

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

    public LaunchServer(Path dir, boolean testEnv, String[] args) throws IOException, InvalidKeySpecException {
        this.dir = dir;
        this.testEnv = testEnv;
        taskPool = new Timer("Timered task worker thread", true);
        launcherLibraries = dir.resolve("launcher-libraries");
        launcherLibrariesCompile = dir.resolve("launcher-libraries-compile");
        this.args = Arrays.asList(args);
        if(IOHelper.exists(dir.resolve("LaunchServer.conf")))
        {
            configFile = dir.resolve("LaunchServer.conf");
        }
        else
        {
            configFile = dir.resolve("LaunchServer.json");
        }
        if(IOHelper.exists(dir.resolve("RuntimeLaunchServer.conf")))
        {
            runtimeConfigFile = dir.resolve("RuntimeLaunchServer.conf");
        }
        else
        {
            runtimeConfigFile = dir.resolve("RuntimeLaunchServer.json");
        }
        publicKeyFile = dir.resolve("public.key");
        privateKeyFile = dir.resolve("private.key");
        updatesDir = dir.resolve("updates");
        profilesDir = dir.resolve("profiles");

        caCertFile = dir.resolve("ca.crt");
        caKeyFile = dir.resolve("ca.key");

        serverCertFile = dir.resolve("server.crt");
        serverKeyFile = dir.resolve("server.key");

        //Registration handlers and providers
        AuthHandler.registerHandlers();
        AuthProvider.registerProviders();
        TextureProvider.registerProviders();
        HWIDHandler.registerHandlers();
        PermissionsHandler.registerHandlers();
        Component.registerComponents();
        ProtectHandler.registerHandlers();
        WebSocketService.registerResponses();
        HWIDProvider.registerHWIDs();
        DaoProvider.registerProviders();
        //LaunchServer.server = this;

        // Set command handler
        CommandHandler localCommandHandler;
        if (testEnv)
            localCommandHandler = new StdCommandHandler(false);
        else
            try {
                Class.forName("org.jline.terminal.Terminal");

                // JLine2 available
                localCommandHandler = new JLineCommandHandler();
                LogHelper.info("JLine2 terminal enabled");
            } catch (ClassNotFoundException ignored) {
                localCommandHandler = new StdCommandHandler(true);
                LogHelper.warning("JLine2 isn't in classpath, using std");
            }
        commandHandler = localCommandHandler;

        // Set key pair
        if (IOHelper.isFile(publicKeyFile) && IOHelper.isFile(privateKeyFile)) {
            LogHelper.info("Reading RSA keypair");
            publicKey = SecurityHelper.toPublicRSAKey(IOHelper.read(publicKeyFile));
            privateKey = SecurityHelper.toPrivateRSAKey(IOHelper.read(privateKeyFile));
            if (!publicKey.getModulus().equals(privateKey.getModulus()))
                throw new IOException("Private and public key modulus mismatch");
        } else {
            LogHelper.info("Generating RSA keypair");
            KeyPair pair = SecurityHelper.genRSAKeyPair();
            publicKey = (RSAPublicKey) pair.getPublic();
            privateKey = (RSAPrivateKey) pair.getPrivate();

            // Write key pair list
            LogHelper.info("Writing RSA keypair list");
            IOHelper.write(publicKeyFile, publicKey.getEncoded());
            IOHelper.write(privateKeyFile, privateKey.getEncoded());
        }

        // Print keypair fingerprints
        CRC32 crc = new CRC32();
        crc.update(publicKey.getModulus().toByteArray()); // IDEA говорит, что это Java 9 API. WTF?
        LogHelper.subInfo("Modulus CRC32: 0x%08x", crc.getValue());

        // Load class bindings.
        launcherEXEBinaryClass = defaultLauncherEXEBinaryClass;

        // pre init modules
        modulesManager = new ModulesManager(this);
        modulesManager.autoload(dir.resolve("modules"));
        modulesManager.preInitModules();
        initGson();

        // Read LaunchServer config
        generateConfigIfNotExists(testEnv);
        LogHelper.info("Reading LaunchServer config file");
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = Launcher.gsonManager.gson.fromJson(reader, LaunchServerConfig.class);
        }
        config.setLaunchServer(this);
        if (!Files.exists(runtimeConfigFile)) {
            LogHelper.info("Reset LaunchServer runtime config file");
            runtime = new LaunchServerRuntimeConfig();
            runtime.reset();
        } else {
            LogHelper.info("Reading LaunchServer runtime config file");
            try (BufferedReader reader = IOHelper.newReader(runtimeConfigFile)) {
                runtime = Launcher.gsonManager.gson.fromJson(reader, LaunchServerRuntimeConfig.class);
            }
        }
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

        pro.gravit.launchserver.command.handler.CommandHandler.registerCommands(localCommandHandler, this);

        // init modules
        modulesManager.initModules();
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

        // post init modules
        modulesManager.postInitModules();
        if (config.components != null) {
            LogHelper.debug("PostInit components");
            config.components.forEach((k, v) -> {
                LogHelper.subDebug("PostInit component %s", k);
                v.postInit(this);
            });
            LogHelper.debug("PostInit components successful");
        }
        // start updater
        if (config.netty != null)
            nettyServerSocketHandler = new NettyServerSocketHandler(this);
        else
            nettyServerSocketHandler = null;
    }

    public static void initGson() {
        Launcher.gsonManager = new LaunchServerGsonManager();
        Launcher.gsonManager.initGson();
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

    public void close() {

        // Close handlers & providers
        config.close(ReloadType.FULL);
        modulesManager.close();
        LogHelper.info("Save LaunchServer runtime config");
        try (Writer writer = IOHelper.newWriter(runtimeConfigFile)) {
            if (Launcher.gsonManager.configGson != null) {
                Launcher.gsonManager.configGson.toJson(runtime, writer);
            } else {
                LogHelper.error("Error writing LaunchServer runtime config file. Gson is null");
            }
        } catch (IOException e) {
            LogHelper.error(e);
        }
        // Print last message before death :(
        LogHelper.info("LaunchServer stopped");
    }

    private void generateConfigIfNotExists(boolean testEnv) throws IOException {
        if (IOHelper.isFile(configFile))
            return;

        // Create new config
        LogHelper.info("Creating LaunchServer config");


        LaunchServerConfig newConfig = LaunchServerConfig.getDefault();
        // Set server address
        String address;
        if (testEnv) {
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
        if (!this.testEnv) {
            JVMHelper.RUNTIME.addShutdownHook(CommonHelper.newThread(null, false, this::close));
            CommonHelper.newThread("Command Thread", true, commandHandler).start();
        }
        if (config.netty != null)
            rebindNettyServerSocket();
        modulesManager.finishModules();
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
