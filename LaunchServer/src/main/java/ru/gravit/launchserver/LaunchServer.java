package ru.gravit.launchserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
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
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.hasher.HashedDir;
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
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.BooleanConfigEntry;
import ru.gravit.launcher.serialize.config.entry.IntegerConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
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
        @LauncherAPI
        public final int port;

        // Handlers & Providers
        @LauncherAPI
        public final AuthHandler authHandler;
        @LauncherAPI
        public final AuthProvider authProvider;
        @LauncherAPI
        public final TextureProvider textureProvider;
        @LauncherAPI
        public final HWIDHandler hwidHandler;

        // Misc options
        @LauncherAPI
        public final ExeConf launch4j;
        @LauncherAPI
        public final SignConf sign;
        @LauncherAPI
        public final boolean compress;
        @LauncherAPI
        public final int authRateLimit;
        @LauncherAPI
        public final int authRateLimitMilis;
        @LauncherAPI
        public final String authRejectString;
        @LauncherAPI
        public final String projectName;
        @LauncherAPI
        public final String whitelistRejectString;
        @LauncherAPI
        public final boolean genMappings;
        @LauncherAPI
        public final String binaryName;
        private final StringConfigEntry address;
        private final String bindAddress;

        private Config(BlockConfigEntry block, Path coredir) {
            super(block);
            address = block.getEntry("address", StringConfigEntry.class);
            port = VerifyHelper.verifyInt(block.getEntryValue("port", IntegerConfigEntry.class),
                    VerifyHelper.range(0, 65535), "Illegal LaunchServer port");
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
            authHandler = AuthHandler.newHandler(block.getEntryValue("authHandler", StringConfigEntry.class),
                    block.getEntry("authHandlerConfig", BlockConfigEntry.class));
            authProvider = AuthProvider.newProvider(block.getEntryValue("authProvider", StringConfigEntry.class),
                    block.getEntry("authProviderConfig", BlockConfigEntry.class));
            textureProvider = TextureProvider.newProvider(block.getEntryValue("textureProvider", StringConfigEntry.class),
                    block.getEntry("textureProviderConfig", BlockConfigEntry.class));
            hwidHandler = HWIDHandler.newHandler(block.getEntryValue("hwidHandler", StringConfigEntry.class),
                    block.getEntry("hwidHandlerConfig", BlockConfigEntry.class));

            // Set misc config
            genMappings = block.getEntryValue("proguardPrintMappings", BooleanConfigEntry.class);
            launch4j = new ExeConf(block.getEntry("launch4J", BlockConfigEntry.class));
            sign = new SignConf(block.getEntry("signing", BlockConfigEntry.class), coredir);
            binaryName = block.getEntryValue("binaryName", StringConfigEntry.class);
            projectName = block.hasEntry("projectName") ? block.getEntryValue("projectName", StringConfigEntry.class) : "Minecraft";
            compress = block.getEntryValue("compress", BooleanConfigEntry.class);
        }

        @LauncherAPI
        public String getAddress() {
            return address.getValue();
        }

        @LauncherAPI
        public String getBindAddress() {
            return bindAddress;
        }

        @LauncherAPI
        public SocketAddress getSocketAddress() {
            return new InetSocketAddress(bindAddress, port);
        }

        @LauncherAPI
        public void setAddress(String address) {
            this.address.setValue(address);
        }

        @LauncherAPI
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
        JVMHelper.verifySystemProperties(LaunchServer.class, true);
        LogHelper.addOutput(IOHelper.WORKING_DIR.resolve("LaunchServer.log"));
        LogHelper.printVersion("LaunchServer");

        // Start LaunchServer
        Instant start = Instant.now();
        try {
            try (LaunchServer lsrv = new LaunchServer(IOHelper.WORKING_DIR, false)) {
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
    @LauncherAPI
    public final Path dir;

    @LauncherAPI
    public final Path configFile;
    @LauncherAPI
    public final Path publicKeyFile;
    @LauncherAPI
    public final Path privateKeyFile;
    @LauncherAPI
    public final Path updatesDir;

    @LauncherAPI
    public final Path profilesDir;
    // Server config
    @LauncherAPI
    public final Config config;

    @LauncherAPI
    public final RSAPublicKey publicKey;
    @LauncherAPI
    public final RSAPrivateKey privateKey;
    @LauncherAPI
    public final boolean portable;
    // Launcher binary
    @LauncherAPI
    public final LauncherBinary launcherBinary;
    @LauncherAPI
    public final LauncherBinary launcherEXEBinary;
    // HWID ban + anti-brutforce
    @LauncherAPI
    public final AuthLimiter limiter;
    @LauncherAPI
    public final SessionManager sessionManager;
    // Server
    @LauncherAPI
    public final ModulesManager modulesManager;

    @LauncherAPI
    public final BuildHookManager buildHookManager;
    @LauncherAPI
    public final ProguardConf proguardConf;

    @LauncherAPI
    public final CommandHandler commandHandler;

    @LauncherAPI
    public final ServerSocketHandler serverSocketHandler;

    private final AtomicBoolean started = new AtomicBoolean(false);

    // Updates and profiles
    private volatile List<SignedObjectHolder<ClientProfile>> profilesList;

    private volatile Map<String, SignedObjectHolder<HashedDir>> updatesDirMap;

    public LaunchServer(Path dir, boolean portable) throws IOException, InvalidKeySpecException {
        //setScriptBindings();
        this.portable = portable;

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

        // Set command handler
        CommandHandler localCommandHandler;
        if (portable)
            localCommandHandler = new StdCommandHandler(this, false);
        else
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
        crc.update(publicKey.getModulus().toByteArray());
        LogHelper.subInfo("Modulus CRC32: 0x%08x", crc.getValue());

        // pre init modules
        modulesManager = new ModulesManager(this);
        modulesManager.autoload(dir.resolve("modules"));
        modulesManager.preInitModules();

        // Read LaunchServer config
        generateConfigIfNotExists();
        LogHelper.info("Reading LaunchServer config file");
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = new Config(TextConfigReader.read(reader, true), dir);
        }
        config.verify();

        // build hooks, anti-brutforce and other
        buildHookManager = new BuildHookManager();
        limiter = new AuthLimiter(this);
        proguardConf = new ProguardConf(this);
        sessionManager = new SessionManager();
        GarbageManager.registerNeedGC(sessionManager);
        GarbageManager.registerNeedGC(limiter);

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

    @LauncherAPI
    public void buildLauncherBinaries() throws IOException {
        launcherBinary.build();
        launcherEXEBinary.build();
    }

    @Override
    public void close() {
        serverSocketHandler.close();

        // Close handlers & providers
        try {
            config.authHandler.close();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        try {
            config.authProvider.close();
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
            newConfig = new Config(TextConfigReader.read(reader, false), dir);
        }

        // Set server address
        if (portable) {
            LogHelper.warning("Setting LaunchServer address to 'localhost'");
            newConfig.setAddress("localhost");
        } else {
            LogHelper.println("LaunchServer address: ");
            newConfig.setAddress(commandHandler.readLine());
        }

        // Write LaunchServer config
        LogHelper.info("Writing LaunchServer config file");
        try (BufferedWriter writer = IOHelper.newWriter(configFile)) {
            TextConfigWriter.write(newConfig.block, writer, true);
        }
    }

    @LauncherAPI
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Collection<SignedObjectHolder<ClientProfile>> getProfiles() {
        return profilesList;
    }

    @LauncherAPI
    public SignedObjectHolder<HashedDir> getUpdateDir(String name) {
        return updatesDirMap.get(name);
    }

    @LauncherAPI
    public Set<Entry<String, SignedObjectHolder<HashedDir>>> getUpdateDirs() {
        return updatesDirMap.entrySet();
    }

    @LauncherAPI
    public void rebindServerSocket() {
        serverSocketHandler.close();
        CommonHelper.newThread("Server Socket Thread", false, serverSocketHandler).start();
    }

    @Override
    public void run() {
        if (started.getAndSet(true))
            throw new IllegalStateException("LaunchServer has been already started");

        // Add shutdown hook, then start LaunchServer
        if (!portable) {
            JVMHelper.RUNTIME.addShutdownHook(CommonHelper.newThread(null, false, this::close));
            CommonHelper.newThread("Command Thread", true, commandHandler).start();
        }
        rebindServerSocket();
    }

    @LauncherAPI
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

    @LauncherAPI
    public void syncProfilesDir() throws IOException {
        LogHelper.info("Syncing profiles dir");
        List<SignedObjectHolder<ClientProfile>> newProfies = new LinkedList<>();
        IOHelper.walk(profilesDir, new ProfilesFileVisitor(newProfies), false);

        // Sort and set new profiles
        newProfies.sort(Comparator.comparing(a -> a.object));
        profilesList = Collections.unmodifiableList(newProfies);
    }

    @LauncherAPI
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
