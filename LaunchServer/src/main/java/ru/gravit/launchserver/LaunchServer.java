package ru.gravit.launchserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.managers.GarbageManager;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.auth.AuthLimiter;
import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.auth.handler.MemoryAuthHandler;
import ru.gravit.launchserver.auth.hwid.AcceptHWIDHandler;
import ru.gravit.launchserver.auth.hwid.HWIDHandler;
import ru.gravit.launchserver.auth.permissions.JsonFilePermissionsHandler;
import ru.gravit.launchserver.auth.permissions.PermissionsHandler;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.RejectAuthProvider;
import ru.gravit.launchserver.binary.*;
import ru.gravit.launchserver.command.handler.CommandHandler;
import ru.gravit.launchserver.command.handler.JLineCommandHandler;
import ru.gravit.launchserver.command.handler.StdCommandHandler;
import ru.gravit.launchserver.config.*;
import ru.gravit.launchserver.manangers.*;
import ru.gravit.launchserver.manangers.hook.AuthHookManager;
import ru.gravit.launchserver.manangers.hook.BuildHookManager;
import ru.gravit.launchserver.manangers.hook.SocketHookManager;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.ServerSocketHandler;
import ru.gravit.launchserver.texture.RequestTextureProvider;
import ru.gravit.launchserver.texture.TextureProvider;
import ru.gravit.utils.helper.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;

public final class LaunchServer implements Runnable {
    public static final class Config {
        public int port;

        private String address;

        private String bindAddress;

        public String projectName;

        public String[] mirrors;

        public String binaryName;

        public LauncherConfig.LauncherEnvironment env;

        // Handlers & Providers

        public AuthProvider[] authProvider;

        public AuthHandler authHandler;

        public PermissionsHandler permissionsHandler;

        public TextureProvider textureProvider;

        public HWIDHandler hwidHandler;

        // Misc options
        public int threadCount;

        public int threadCoreCount;

        public ExeConf launch4j;

        public boolean compress;

        public int authRateLimit;

        public int authRateLimitMilis;

        public String[] authLimitExclusions;

        public String authRejectString;

        public String whitelistRejectString;

        public boolean genMappings;
        public boolean isUsingWrapper;
        public boolean isDownloadJava;

        public boolean isWarningMissArchJava;
        public boolean enabledProGuard;
        public boolean stripLineNumbers;
        public boolean deleteTempFiles;
        public boolean enableRcon;

        public String startScript;


        public String getAddress() {
            return address;
        }


        public String getBindAddress() {
            return bindAddress;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public void setBinaryName(String binaryName) {
            this.binaryName = binaryName;
        }

        public void setEnv(LauncherConfig.LauncherEnvironment env) {
            this.env = env;
        }


        public SocketAddress getSocketAddress() {
            return new InetSocketAddress(bindAddress, port);
        }


        public void setAddress(String address) {
            this.address = address;
        }


        public void verify() {
            VerifyHelper.verify(getAddress(), VerifyHelper.NOT_EMPTY, "LaunchServer address can't be empty");
            if (authHandler == null) {
                throw new NullPointerException("AuthHandler must not be null");
            }
            if (authProvider == null || authProvider[0] == null) {
                throw new NullPointerException("AuthProvider must not be null");
            }
            if (textureProvider == null) {
                throw new NullPointerException("TextureProvider must not be null");
            }
            if (permissionsHandler == null) {
                throw new NullPointerException("PermissionsHandler must not be null");
            }
            if (env == null) {
                throw new NullPointerException("Env must not be null");
            }
        }
    }

    public static class ExeConf {
        public boolean enabled;
        public String productName;
        public String productVer;
        public String fileDesc;
        public String fileVer;
        public String internalName;
        public String copyright;
        public String trademarks;

        public String txtFileVersion;
        public String txtProductVersion;
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
                profile = Launcher.gson.fromJson(reader, ClientProfile.class);
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

        // Start LaunchServer
        Instant start = Instant.now();
        try {
            new LaunchServer(IOHelper.WORKING_DIR, args).run();
        } catch (Throwable exc) {
            LogHelper.error(exc);
            return;
        }
        Instant end = Instant.now();
        LogHelper.debug("LaunchServer started in %dms", Duration.between(start, end).toMillis());
    }

    // Constant paths

    public final Path dir;

    public final Path launcherLibraries;

    public final List<String> args;

    public final Path configFile;

    public final Path publicKeyFile;

    public final Path privateKeyFile;

    public final Path updatesDir;

    public static LaunchServer server = null;

    public final Path profilesDir;
    // Server config

    public final Config config;


    public final RSAPublicKey publicKey;

    public final RSAPrivateKey privateKey;
    // Launcher binary

    public final JARLauncherBinary launcherBinary;

    public final LauncherBinary launcherEXEBinary;
    // HWID ban + anti-brutforce

    public final AuthLimiter limiter;

    public final SessionManager sessionManager;

    public final SocketHookManager socketHookManager;

    public final AuthHookManager authHookManager;
    // Server

    public final ModulesManager modulesManager;

    public final MirrorManager mirrorManager;

    public final ReloadManager reloadManager;

    public final ReconfigurableManager reconfigurableManager;


    public final BuildHookManager buildHookManager;

    public final ProguardConf proguardConf;


    public final CommandHandler commandHandler;

    public final ServerSocketHandler serverSocketHandler;

    private final AtomicBoolean started = new AtomicBoolean(false);

    // Updates and profiles
    private volatile List<ClientProfile> profilesList;

    public volatile Map<String, SignedObjectHolder<HashedDir>> updatesDirMap;

    public static Gson gson;
    public static GsonBuilder gsonBuilder;

    public LaunchServer(Path dir, String[] args) throws IOException, InvalidKeySpecException {
        this.dir = dir;
        launcherLibraries = dir.resolve("launcher-libraries");
        if (!Files.isDirectory(launcherLibraries)) {
            Files.deleteIfExists(launcherLibraries);
            Files.createDirectory(launcherLibraries);
        }
        this.args = Arrays.asList(args);
        configFile = dir.resolve("LaunchServer.conf");
        publicKeyFile = dir.resolve("public.key");
        privateKeyFile = dir.resolve("private.key");
        updatesDir = dir.resolve("updates");
        profilesDir = dir.resolve("profiles");

        //Registration handlers and providers
        AuthHandler.registerHandlers();
        AuthProvider.registerProviders();
        TextureProvider.registerProviders();
        HWIDHandler.registerHandlers();
        PermissionsHandler.registerHandlers();
        Response.registerResponses();
        LaunchServer.server = this;

        // Set command handler
        CommandHandler localCommandHandler;
        try {
            Class.forName("jline.Terminal");

            // JLine2 available
            localCommandHandler = new JLineCommandHandler(this);
            LogHelper.info("JLine2 terminal enabled");
        } catch (ClassNotFoundException ignored) {
            localCommandHandler = new StdCommandHandler(this, true);
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

            // Write key pair files
            LogHelper.info("Writing RSA keypair files");
            IOHelper.write(publicKeyFile, publicKey.getEncoded());
            IOHelper.write(privateKeyFile, privateKey.getEncoded());
        }

        // Print keypair fingerprints
        CRC32 crc = new CRC32();
        crc.update(publicKey.getModulus().toByteArray()); // IDEA говорит, что это Java 9 API. WTF?
        LogHelper.subInfo("Modulus CRC32: 0x%08x", crc.getValue());

        // pre init modules
        modulesManager = new ModulesManager(this);
        modulesManager.autoload(dir.resolve("modules"));
        modulesManager.preInitModules();
        initGson();

        // Read LaunchServer config
        generateConfigIfNotExists();
        LogHelper.info("Reading LaunchServer config file");
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = Launcher.gson.fromJson(reader, Config.class);
        }
        config.verify();
        Launcher.applyLauncherEnv(config.env);
        for (AuthProvider provider : config.authProvider) {
            provider.init();
        }
        config.authHandler.init();

        // build hooks, anti-brutforce and other
        buildHookManager = new BuildHookManager();
        limiter = new AuthLimiter(this);
        proguardConf = new ProguardConf(this);
        sessionManager = new SessionManager();
        mirrorManager = new MirrorManager();
        reloadManager = new ReloadManager();
        reconfigurableManager = new ReconfigurableManager();
        socketHookManager = new SocketHookManager();
        authHookManager = new AuthHookManager();
        GarbageManager.registerNeedGC(sessionManager);
        GarbageManager.registerNeedGC(limiter);
        if (config.permissionsHandler instanceof Reloadable)
            reloadManager.registerReloadable("permissionsHandler", (Reloadable) config.permissionsHandler);
        if (config.authHandler instanceof Reloadable)
            reloadManager.registerReloadable("authHandler", (Reloadable) config.authHandler);
        for (int i = 0; i < config.authProvider.length; ++i) {
            AuthProvider provider = config.authProvider[i];
            if (provider instanceof Reloadable)
                reloadManager.registerReloadable("authHandler".concat(String.valueOf(i)), (Reloadable) provider);
        }
        if (config.textureProvider instanceof Reloadable)
            reloadManager.registerReloadable("textureProvider", (Reloadable) config.textureProvider);

        Arrays.stream(config.mirrors).forEach(mirrorManager::addMirror);

        if (config.permissionsHandler instanceof Reconfigurable)
            reconfigurableManager.registerReconfigurable("permissionsHandler", (Reconfigurable) config.permissionsHandler);
        if (config.authHandler instanceof Reconfigurable)
            reconfigurableManager.registerReconfigurable("authHandler", (Reconfigurable) config.authHandler);
        for (int i = 0; i < config.authProvider.length; ++i) {
            AuthProvider provider = config.authProvider[i];
            if (provider instanceof Reconfigurable)
                reconfigurableManager.registerReconfigurable("authHandler".concat(String.valueOf(i)), (Reconfigurable) provider);
        }
        if (config.textureProvider instanceof Reconfigurable)
            reconfigurableManager.registerReconfigurable("textureProvider", (Reconfigurable) config.textureProvider);

        Arrays.stream(config.mirrors).forEach(mirrorManager::addMirror);

        // init modules
        modulesManager.initModules();

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


        // Set server socket thread
        serverSocketHandler = new ServerSocketHandler(this, sessionManager);

        // post init modules
        modulesManager.postInitModules();
    }

    public static void initGson() {
        if (Launcher.gson != null) return;
        Launcher.gsonBuilder = new GsonBuilder();
        Launcher.gsonBuilder.registerTypeAdapter(AuthProvider.class, new AuthProviderAdapter());
        Launcher.gsonBuilder.registerTypeAdapter(TextureProvider.class, new TextureProviderAdapter());
        Launcher.gsonBuilder.registerTypeAdapter(AuthHandler.class, new AuthHandlerAdapter());
        Launcher.gsonBuilder.registerTypeAdapter(PermissionsHandler.class, new PermissionsHandlerAdapter());
        Launcher.gsonBuilder.registerTypeAdapter(HWIDHandler.class, new HWIDHandlerAdapter());
        Launcher.gson = Launcher.gsonBuilder.create();

        //Human readable
        LaunchServer.gsonBuilder = new GsonBuilder();
        LaunchServer.gsonBuilder.setPrettyPrinting();
        LaunchServer.gsonBuilder.registerTypeAdapter(AuthProvider.class, new AuthProviderAdapter());
        LaunchServer.gsonBuilder.registerTypeAdapter(TextureProvider.class, new TextureProviderAdapter());
        LaunchServer.gsonBuilder.registerTypeAdapter(AuthHandler.class, new AuthHandlerAdapter());
        LaunchServer.gsonBuilder.registerTypeAdapter(PermissionsHandler.class, new PermissionsHandlerAdapter());
        LaunchServer.gsonBuilder.registerTypeAdapter(HWIDHandler.class, new HWIDHandlerAdapter());
        LaunchServer.gson = LaunchServer.gsonBuilder.create();
    }

    private LauncherBinary binary() {
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
        serverSocketHandler.close();

        // Close handlers & providers
        try {
            config.authHandler.close();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        try {
            for (AuthProvider p : config.authProvider) p.close();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        try {
            config.textureProvider.close();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        config.hwidHandler.close();
        modulesManager.close();
        // Print last message before death :(
        LogHelper.info("LaunchServer stopped");
    }

    private void generateConfigIfNotExists() throws IOException {
        if (IOHelper.isFile(configFile))
            return;

        // Create new config
        LogHelper.info("Creating LaunchServer config");
        Config newConfig = new Config();
        newConfig.mirrors = new String[]{"http://mirror.gravitlauncher.ml/"};
        newConfig.launch4j = new ExeConf();
        newConfig.launch4j.copyright = "© GravitLauncher Team";
        newConfig.launch4j.fileDesc = "GravitLauncher ".concat(Launcher.getVersion().getVersionString());
        newConfig.launch4j.fileVer = Launcher.getVersion().getVersionString().concat(".").concat(String.valueOf(Launcher.getVersion().patch));
        newConfig.launch4j.internalName = "Launcher";
        newConfig.launch4j.trademarks = "This product is licensed under GPLv3";
        newConfig.launch4j.txtFileVersion = "%s, build %d";
        newConfig.launch4j.txtProductVersion = "%s, build %d";
        newConfig.launch4j.productName = "GravitLauncher";
        newConfig.launch4j.productVer = newConfig.launch4j.fileVer;
        newConfig.env = LauncherConfig.LauncherEnvironment.STD;
        newConfig.startScript = JVMHelper.OS_TYPE.equals(JVMHelper.OS.MUSTDIE) ? "." + File.separator + "start.bat" : "." + File.separator + "start.sh";
        newConfig.authHandler = new MemoryAuthHandler();
        newConfig.hwidHandler = new AcceptHWIDHandler();

        newConfig.authProvider = new AuthProvider[]{new RejectAuthProvider("Настройте authProvider")};
        newConfig.textureProvider = new RequestTextureProvider("http://example.com/skins/%username%.png", "http://example.com/cloaks/%username%.png");
        newConfig.permissionsHandler = new JsonFilePermissionsHandler();
        newConfig.port = 7240;
        newConfig.bindAddress = "0.0.0.0";
        newConfig.authRejectString = "Превышен лимит авторизаций";
        newConfig.binaryName = "Launcher";
        newConfig.whitelistRejectString = "Вас нет в белом списке";

        newConfig.threadCoreCount = 0; // on your own
        newConfig.threadCount = JVMHelper.OPERATING_SYSTEM_MXBEAN.getAvailableProcessors() >= 4 ? JVMHelper.OPERATING_SYSTEM_MXBEAN.getAvailableProcessors() / 2 : JVMHelper.OPERATING_SYSTEM_MXBEAN.getAvailableProcessors();

        newConfig.enabledProGuard = true;
        newConfig.stripLineNumbers = true;
        newConfig.deleteTempFiles = true;
        newConfig.isWarningMissArchJava = true;

        // Set server address
        LogHelper.println("LaunchServer address: ");
        newConfig.setAddress(commandHandler.readLine());
        LogHelper.println("LaunchServer projectName: ");
        newConfig.setProjectName(commandHandler.readLine());

        // Write LaunchServer config
        LogHelper.info("Writing LaunchServer config file");
        try (BufferedWriter writer = IOHelper.newWriter(configFile)) {
            LaunchServer.gson.toJson(newConfig, writer);
        }
    }

    public Collection<ClientProfile> getProfiles() {
        return profilesList;
    }


    public SignedObjectHolder<HashedDir> getUpdateDir(String name) {
        return updatesDirMap.get(name);
    }


    public Set<Entry<String, SignedObjectHolder<HashedDir>>> getUpdateDirs() {
        return updatesDirMap.entrySet();
    }


    public void rebindServerSocket() {
        serverSocketHandler.close();
        CommonHelper.newThread("Server Socket Thread", false, serverSocketHandler).start();
    }

    @Override
    public void run() {
        if (started.getAndSet(true))
            throw new IllegalStateException("LaunchServer has been already started");

        // Add shutdown hook, then start LaunchServer
        JVMHelper.RUNTIME.addShutdownHook(CommonHelper.newThread(null, false, this::close));
        CommonHelper.newThread("Command Thread", true, commandHandler).start();
        rebindServerSocket();
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
        Map<String, SignedObjectHolder<HashedDir>> newUpdatesDirMap = new HashMap<>(16);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(updatesDir)) {
            for (Path updateDir : dirStream) {
                if (Files.isHidden(updateDir))
                    continue; // Skip hidden

                // Resolve name and verify is dir
                String name = IOHelper.getFileName(updateDir);
                if (!IOHelper.isDir(updateDir)) {
                    LogHelper.warning("Not update dir: '%s'", name);
                    continue;
                }

                // Add from previous map (it's guaranteed to be non-null)
                if (dirs != null && !dirs.contains(name)) {
                    SignedObjectHolder<HashedDir> hdir = updatesDirMap.get(name);
                    if (hdir != null) {
                        newUpdatesDirMap.put(name, hdir);
                        continue;
                    }
                }

                // Sync and sign update dir
                LogHelper.info("Syncing '%s' update dir", name);
                HashedDir updateHDir = new HashedDir(updateDir, null, true, true);
                newUpdatesDirMap.put(name, new SignedObjectHolder<>(updateHDir, privateKey));
            }
        }
        updatesDirMap = Collections.unmodifiableMap(newUpdatesDirMap);
    }

    public void restart() {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> args = new ArrayList<>();
        if (config.startScript != null) args.add(config.startScript);
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

    public void fullyRestart() {
        restart();
        JVMHelper.RUNTIME.exit(0);
    }
}
