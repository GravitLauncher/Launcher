package ru.gravit.launchserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.serialize.config.entry.*;
import ru.gravit.launchserver.manangers.MirrorManager;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.managers.GarbageManager;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.TextConfigReader;
import ru.gravit.launcher.serialize.config.TextConfigWriter;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.auth.AuthLimiter;
import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.auth.hwid.HWIDHandler;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.binary.EXEL4JLauncherBinary;
import ru.gravit.launchserver.binary.EXELauncherBinary;
import ru.gravit.launchserver.binary.JARLauncherBinary;
import ru.gravit.launchserver.binary.LauncherBinary;
import ru.gravit.launchserver.command.handler.CommandHandler;
import ru.gravit.launchserver.command.handler.JLineCommandHandler;
import ru.gravit.launchserver.command.handler.StdCommandHandler;
import ru.gravit.launchserver.manangers.BuildHookManager;
import ru.gravit.launchserver.manangers.ModulesManager;
import ru.gravit.launchserver.manangers.SessionManager;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.ServerSocketHandler;
import ru.gravit.launchserver.texture.TextureProvider;

public final class LaunchServer implements Runnable, AutoCloseable {
    public static final class Config extends ConfigObject {
        public final int port;

        // Handlers & Providers

        public final AuthHandler[] authHandler;

        public final AuthProvider[] authProvider;

        public final TextureProvider textureProvider;

        public final HWIDHandler hwidHandler;

        // Misc options
    	public final int threadCount;
  
    	public final int threadCoreCount;
    	
        public final ExeConf launch4j;

        public final SignConf sign;

        public final boolean compress;

        public final int authRateLimit;

        public final int authRateLimitMilis;

        public final String authRejectString;

        public final String projectName;

        public final String whitelistRejectString;

        public final boolean genMappings;
        public final boolean isUsingWrapper;
        public final boolean isDownloadJava;

        public ListConfigEntry mirrors;
        public final String binaryName;
        private final StringConfigEntry address;
        private final String bindAddress;

        private Config(BlockConfigEntry block, Path coredir, LaunchServer server) {
            super(block);
            address = block.getEntry("address", StringConfigEntry.class);
            port = VerifyHelper.verifyInt(block.getEntryValue("port", IntegerConfigEntry.class),
                    VerifyHelper.range(0, 65535), "Illegal LaunchServer port");
            threadCoreCount = block.hasEntry("threadCoreCacheSize") ? VerifyHelper.verifyInt(block.getEntryValue("threadCoreCacheSize", IntegerConfigEntry.class),
                    VerifyHelper.range(0, 100), "Illegal LaunchServer inital thread pool cache size") : 0;
            int internalThreadCount = block.hasEntry("threadCacheSize") ? VerifyHelper.verifyInt(block.getEntryValue("threadCacheSize", IntegerConfigEntry.class),
                    VerifyHelper.range(2, 100), "Illegal LaunchServer thread pool cache size") : (JVMHelper.OPERATING_SYSTEM_MXBEAN.getAvailableProcessors() >= 4 ? JVMHelper.OPERATING_SYSTEM_MXBEAN.getAvailableProcessors() / 2 : JVMHelper.OPERATING_SYSTEM_MXBEAN.getAvailableProcessors());
            threadCount = threadCoreCount > internalThreadCount ? threadCoreCount : internalThreadCount;
            authRateLimit = VerifyHelper.verifyInt(block.getEntryValue("authRateLimit", IntegerConfigEntry.class),
                    VerifyHelper.range(0, 1000000), "Illegal authRateLimit");
            authRateLimitMilis = VerifyHelper.verifyInt(block.getEntryValue("authRateLimitMilis", IntegerConfigEntry.class),
                    VerifyHelper.range(10, 10000000), "Illegal authRateLimitMillis");
            bindAddress = block.hasEntry("bindAddress") ?
                    block.getEntryValue("bindAddress", StringConfigEntry.class) : getAddress();
            authRejectString = block.hasEntry("authRejectString") ?
                    block.getEntryValue("authRejectString", StringConfigEntry.class) : "Вы превысили лимит авторизаций. Подождите некоторое время перед повторной попыткой";
            whitelistRejectString = block.hasEntry("whitelistRejectString") ?
                    block.getEntryValue("whitelistRejectString", StringConfigEntry.class) : "Вас нет в белом списке";


            // Set handlers & providers
            authHandler = new AuthHandler[1];
            authHandler[0] = AuthHandler.newHandler(block.getEntryValue("authHandler", StringConfigEntry.class),
                    block.getEntry("authHandlerConfig", BlockConfigEntry.class));
            authProvider = new AuthProvider[1];
            authProvider[0] = AuthProvider.newProvider(block.getEntryValue("authProvider", StringConfigEntry.class),
                    block.getEntry("authProviderConfig", BlockConfigEntry.class), server);
            textureProvider = TextureProvider.newProvider(block.getEntryValue("textureProvider", StringConfigEntry.class),
                    block.getEntry("textureProviderConfig", BlockConfigEntry.class));
            hwidHandler = HWIDHandler.newHandler(block.getEntryValue("hwidHandler", StringConfigEntry.class),
                    block.getEntry("hwidHandlerConfig", BlockConfigEntry.class));

            // Set misc config
            genMappings = block.getEntryValue("proguardPrintMappings", BooleanConfigEntry.class);
            mirrors = block.getEntry("mirrors", ListConfigEntry.class);
            launch4j = new ExeConf(block.getEntry("launch4J", BlockConfigEntry.class));
            sign = new SignConf(block.getEntry("signing", BlockConfigEntry.class), coredir);
            binaryName = block.getEntryValue("binaryName", StringConfigEntry.class);
            projectName = block.hasEntry("projectName") ? block.getEntryValue("projectName", StringConfigEntry.class) : "Minecraft";
            compress = block.getEntryValue("compress", BooleanConfigEntry.class);

            isUsingWrapper = block.getEntryValue("isUsingWrapper", BooleanConfigEntry.class);
            isDownloadJava = block.getEntryValue("isDownloadJava", BooleanConfigEntry.class);
        }


        public String getAddress() {
            return address.getValue();
        }


        public String getBindAddress() {
            return bindAddress;
        }


        public SocketAddress getSocketAddress() {
            return new InetSocketAddress(bindAddress, port);
        }


        public void setAddress(String address) {
            this.address.setValue(address);
        }


        public void verify() {
            VerifyHelper.verify(getAddress(), VerifyHelper.NOT_EMPTY, "LaunchServer address can't be empty");
        }
    }

    public static class ExeConf extends ConfigObject {
        public final boolean enabled;
        public String productName;
        public String productVer;
        public String fileDesc;
        public String fileVer;
        public String internalName;
        public String copyright;
        public String trademarks;

        public String txtFileVersion;
        public String txtProductVersion;

        private ExeConf(BlockConfigEntry block) {
            super(block);
            enabled = block.getEntryValue("enabled", BooleanConfigEntry.class);
            productName = block.hasEntry("productName") ? block.getEntryValue("productName", StringConfigEntry.class)
                    : "sashok724's Launcher v3 mod by Gravit";
            productVer = block.hasEntry("productVer") ? block.getEntryValue("productVer", StringConfigEntry.class)
                    : "1.0.0.0";
            fileDesc = block.hasEntry("fileDesc") ? block.getEntryValue("fileDesc", StringConfigEntry.class)
                    : "sashok724's Launcher v3 mod by Gravit";
            fileVer = block.hasEntry("fileVer") ? block.getEntryValue("fileVer", StringConfigEntry.class) : "1.0.0.0";
            internalName = block.hasEntry("internalName") ? block.getEntryValue("internalName", StringConfigEntry.class)
                    : "Launcher";
            copyright = block.hasEntry("copyright") ? block.getEntryValue("copyright", StringConfigEntry.class)
                    : "© sashok724 LLC";
            trademarks = block.hasEntry("trademarks") ? block.getEntryValue("trademarks", StringConfigEntry.class)
                    : "This product is licensed under MIT License";
            txtFileVersion = block.hasEntry("txtFileVersion") ? block.getEntryValue("txtFileVersion", StringConfigEntry.class)
                    : String.format("%s, build %d", Launcher.getVersion().getVersionString(), Launcher.getVersion().build);
            txtProductVersion = block.hasEntry("txtProductVersion") ? block.getEntryValue("txtProductVersion", StringConfigEntry.class)
                    : String.format("%s, build %d", Launcher.getVersion().getVersionString(), Launcher.getVersion().build);
        }
    }

    private final class ProfilesFileVisitor extends SimpleFileVisitor<Path> {
        private final Collection<SignedObjectHolder<ClientProfile>> result;

        private ProfilesFileVisitor(Collection<SignedObjectHolder<ClientProfile>> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            LogHelper.info("Syncing '%s' profile", IOHelper.getFileName(file));

            // Read profile
            ClientProfile profile;
            try (BufferedReader reader = IOHelper.newReader(file)) {
                profile = new ClientProfile(TextConfigReader.read(reader, true));
            }
            profile.verify();

            // Add SIGNED profile to result list
            result.add(new SignedObjectHolder<>(profile, privateKey));
            return super.visitFile(file, attrs);
        }
    }

    public static class SignConf extends ConfigObject {
        public final boolean enabled;
        public String algo;
        public Path key;
        public boolean hasStorePass;
        public String storepass;
        public boolean hasPass;
        public String pass;
        public String keyalias;

        private SignConf(BlockConfigEntry block, Path coredir) {
            super(block);
            enabled = block.getEntryValue("enabled", BooleanConfigEntry.class);
            storepass = null;
            pass = null;
            if (enabled) {
                algo = block.hasEntry("storeType") ? block.getEntryValue("storeType", StringConfigEntry.class) : "JKS";
                key = coredir.resolve(block.getEntryValue("keyFile", StringConfigEntry.class));
                hasStorePass = block.hasEntry("keyStorePass");
                if (hasStorePass) storepass = block.getEntryValue("keyStorePass", StringConfigEntry.class);
                keyalias = block.getEntryValue("keyAlias", StringConfigEntry.class);
                hasPass = block.hasEntry("keyPass");
                if (hasPass) pass = block.getEntryValue("keyPass", StringConfigEntry.class);
            }
        }
    }

    public static void main(String... args) throws Throwable {
        JVMHelper.checkStackTrace(LaunchServer.class);
        JVMHelper.verifySystemProperties(LaunchServer.class, true);
        LogHelper.addOutput(IOHelper.WORKING_DIR.resolve("LaunchServer.log"));
        LogHelper.printVersion("LaunchServer");

        // Start LaunchServer
        Instant start = Instant.now();
        try {
            try (LaunchServer lsrv = new LaunchServer(IOHelper.WORKING_DIR)) {
                lsrv.run();
            }
        } catch (Throwable exc) {
            LogHelper.error(exc);
            return;
        }
        Instant end = Instant.now();
        LogHelper.debug("LaunchServer started in %dms", Duration.between(start, end).toMillis());
    }

    // Constant paths

    public final Path dir;


    public final Path configFile;

    public final Path publicKeyFile;

    public final Path privateKeyFile;

    public final Path updatesDir;
    public static LaunchServer server;

    public final Path profilesDir;
    // Server config

    public final Config config;


    public final RSAPublicKey publicKey;

    public final RSAPrivateKey privateKey;
    // Launcher binary

    public final LauncherBinary launcherBinary;

    public final LauncherBinary launcherEXEBinary;
    // HWID ban + anti-brutforce

    public final AuthLimiter limiter;

    public final SessionManager sessionManager;
    // Server

    public final ModulesManager modulesManager;

    public final MirrorManager mirrorManager;


    public final BuildHookManager buildHookManager;

    public final ProguardConf proguardConf;


    public final CommandHandler commandHandler;


    public final ServerSocketHandler serverSocketHandler;

    private final AtomicBoolean started = new AtomicBoolean(false);

    // Updates and profiles
    private volatile List<SignedObjectHolder<ClientProfile>> profilesList;

    public volatile Map<String, SignedObjectHolder<HashedDir>> updatesDirMap;

    public LaunchServer(Path dir) throws IOException, InvalidKeySpecException {
        // Setup config locations
        this.dir = dir;
        configFile = dir.resolve("LaunchServer.cfg");
        publicKeyFile = dir.resolve("public.key");
        privateKeyFile = dir.resolve("private.key");
        updatesDir = dir.resolve("updates");
        profilesDir = dir.resolve("profiles");

        //Registration handlers and providers
        AuthHandler.registerHandlers();
        AuthProvider.registerProviders();
        TextureProvider.registerProviders();
        HWIDHandler.registerHandlers();
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

        // Read LaunchServer config
        generateConfigIfNotExists();
        LogHelper.info("Reading LaunchServer config file");
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = new Config(TextConfigReader.read(reader, true), dir, this);
        }
        config.verify();

        // build hooks, anti-brutforce and other
        buildHookManager = new BuildHookManager();
        limiter = new AuthLimiter(this);
        proguardConf = new ProguardConf(this);
        sessionManager = new SessionManager();
        mirrorManager = new MirrorManager();
        GarbageManager.registerNeedGC(sessionManager);
        GarbageManager.registerNeedGC(limiter);
        config.mirrors.stream(StringConfigEntry.class).forEach(s -> {
            try {
                mirrorManager.addMirror(s);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });

        // init modules
        modulesManager.initModules();

        // Set launcher EXE binary
        launcherBinary = new JARLauncherBinary(this);
        launcherEXEBinary = binary();
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

    private LauncherBinary binary() {
        if (config.launch4j.enabled) return new EXEL4JLauncherBinary(this);
        return new EXELauncherBinary(this);
    }


    public void buildLauncherBinaries() throws IOException {
        launcherBinary.build();
        launcherEXEBinary.build();
    }

    @Override
    public void close() {
        serverSocketHandler.close();

        // Close handlers & providers
        try {
            for (AuthHandler h : config.authHandler) h.close();
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
        Config newConfig;
        try (BufferedReader reader = IOHelper.newReader(IOHelper.getResourceURL("ru/gravit/launchserver/defaults/config.cfg"))) {
            newConfig = new Config(TextConfigReader.read(reader, false), dir, this);
        }

        // Set server address
        LogHelper.println("LaunchServer address: ");
        newConfig.setAddress(commandHandler.readLine());

        // Write LaunchServer config
        LogHelper.info("Writing LaunchServer config file");
        try (BufferedWriter writer = IOHelper.newWriter(configFile)) {
            TextConfigWriter.write(newConfig.block, writer, true);
        }
    }


    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<SignedObjectHolder<ClientProfile>> getProfiles() {
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
        List<SignedObjectHolder<ClientProfile>> newProfies = new LinkedList<>();
        IOHelper.walk(profilesDir, new ProfilesFileVisitor(newProfies), false);

        // Sort and set new profiles
        newProfies.sort(Comparator.comparing(a -> a.object));
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
}
