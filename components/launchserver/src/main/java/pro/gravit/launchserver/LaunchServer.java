package pro.gravit.launchserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.modules.events.ClosePhase;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.distribution.UpdatesProvider;
import pro.gravit.launchserver.binary.JARLauncherBinary;
import pro.gravit.launchserver.binary.LauncherBinary;
import pro.gravit.launchserver.binary.PipelineContext;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.config.LauncherModulesConfig;
import pro.gravit.launchserver.helper.SignHelper;
import pro.gravit.launchserver.launchermodules.LauncherModuleLoader;
import pro.gravit.launchserver.manangers.*;
import pro.gravit.launchserver.modules.events.*;
import pro.gravit.launchserver.modules.impl.LaunchServerModulesManager;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.*;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
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
    public final Path librariesDir;
    public final Path controlFile;
    public final Path proguardDir;
    /**
     * Pipeline for building JAR
     */
    public final Map<CoreFeatureAPI.UpdateVariant, LauncherBinary> launcherBinaries;
    // Server config
    public final LaunchServerModulesManager modulesManager;
    // Launcher binary
    public final MirrorManager mirrorManager;
    public final ReconfigurableManager reconfigurableManager;
    public final ConfigManager configManager;
    public final KeyAgreementManager keyAgreementManager;
    // HWID ban + anti-brutforce
    public final CertificateManager certificateManager;
    // Server
    public final CommandHandler commandHandler;
    public final ScheduledExecutorService service;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final CountDownLatch termination = new CountDownLatch(1);
    public final LauncherModuleLoader launcherModuleLoader;
    private final Logger logger = LogManager.getLogger();
    public final int shardId;
    public volatile LaunchServerConfig config;
    public final LauncherModulesConfig modulesConfig;

    public LaunchServer(LaunchServerDirectories directories, LaunchServerEnv env, LaunchServerConfig config, LaunchServerConfigManager launchServerConfigManager, LaunchServerModulesManager modulesManager, KeyAgreementManager keyAgreementManager, CommandHandler commandHandler, CertificateManager certificateManager, int shardId, LauncherModulesConfig modulesConfig) throws IOException {
        this.dir = directories.dir;
        this.tmpDir = directories.tmpDir;
        this.env = env;
        this.config = config;
        this.launchServerConfigManager = launchServerConfigManager;
        this.modulesManager = modulesManager;
        this.updatesDir = directories.updatesDir;
        this.keyAgreementManager = keyAgreementManager;
        this.commandHandler = commandHandler;
        this.certificateManager = certificateManager;
        config.verify();
        launcherLibraries = directories.launcherLibrariesDir;
        launcherLibrariesCompile = directories.launcherLibrariesCompileDir;
        launcherPack = directories.launcherPackDir;
        modulesDir = directories.modules;
        librariesDir = directories.librariesDir;
        controlFile = directories.controlFile;
        proguardDir = directories.proguardDir;
        this.shardId = shardId;
        this.modulesConfig = modulesConfig;
        if(!Files.isDirectory(launcherPack)) {
            Files.createDirectories(launcherPack);
        }

        config.setLaunchServer(this);


        if(config.experimentalDevOnlyFeatures) {
            logger.warn("Experimental features enabled. Be careful");
        }

        // build hooks, anti-brutforce and other
        mirrorManager = new MirrorManager();
        reconfigurableManager = new ReconfigurableManager();
        configManager = new ConfigManager();

        this.service = Executors.newScheduledThreadPool(config.scheduler.threads);
        LauncherModuleLoader moduleLoader = null;
        try {
            modulesManager.invokeEvent(new NewLaunchServerInstanceEvent(this));
            config.init(ReloadType.FULL);
            registerObject("launchServer", this);

            pro.gravit.launchserver.command.handler.CommandHandler.registerCommands(commandHandler, this);

            // init modules
            modulesManager.invokeEvent(new LaunchServerInitPhase(this));

            // Set launcher EXE binary
            launcherBinaries = new HashMap<>();
            collectBinary();
            moduleLoader = new LauncherModuleLoader(this);
            launcherModuleLoader = moduleLoader;
            config.initComponents();
            moduleLoader.init();
            if(config.sign.checkCertificateExpired) {
                checkCertificateExpired();
                service.scheduleAtFixedRate(this::checkCertificateExpired, 24, 24, TimeUnit.HOURS);
            }
            // post init modules
            modulesManager.invokeEvent(new LaunchServerPostInitPhase(this));
        } catch (IOException | RuntimeException | Error failure) {
            service.shutdownNow();
            try { config.close(ReloadType.FULL); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            try { mirrorManager.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            try { modulesManager.invokeEvent(new ClosePhase()); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            if (moduleLoader != null) {
                try { moduleLoader.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            }
            try { modulesManager.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public synchronized void reload(ReloadType type) throws Exception {
        if (closed.get()) throw new IllegalStateException("LaunchServer is closed");
        logger.info("Reading LaunchServer config file");
        LaunchServerConfig replacement = launchServerConfigManager.readConfig();
        replacement.setLaunchServer(this);
        replacement.verify();
        if (replacement.scheduler.threads != config.scheduler.threads) {
            throw new IllegalArgumentException("Changing scheduler.threads requires a restart");
        }
        if (replacement == config) return;
        LaunchServerConfig previous = config;
        if (type == ReloadType.NO_COMPONENTS) replacement.components = previous.components;
        try {
            previous.close(type);
        } catch (Exception | Error failure) {
            try { close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
        if (type == ReloadType.NO_COMPONENTS) replacement.inheritComponents(previous);
        config = replacement;
        mirrorManager.clear();
        try {
            config.init(type);
            if (type == ReloadType.FULL) config.initComponents();
        } catch (Exception | Error failure) {
            try { close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
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
                    default -> throw new IllegalArgumentException("Reload type must be full or no_components");
                }
            }
        };
        commands.put("reload", reload);
        SubCommand save = new SubCommand("[]", "save launchserver config") {
            @Override
            public void invoke(String... args) throws Exception {
                launchServerConfigManager.writeConfig(config);
                logger.info("LaunchServerConfig saved");
            }
        };
        commands.put("save", save);
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

    public Path createTempDirectory(String name) throws IOException {
        var path = tmpDir.resolve(String.format("launchserver-%s-%s", name, SecurityHelper.randomStringToken()));
        Files.createDirectories(path);
        return path;
    }

    public Path createTempFilePath(String name, String ext) throws IOException {
        var path = tmpDir.resolve(String.format("launchserver-%s-%s.%s", name, SecurityHelper.randomStringToken(), ext));
        IOHelper.createParentDirs(path);
        return path;
    }

    private void collectBinary() throws IOException {
        launcherBinaries.clear();
        launcherBinaries.put(CoreFeatureAPI.UpdateVariant.JAR, new JARLauncherBinary(this));
        LaunchServerLauncherBinaryInit event = new LaunchServerLauncherBinaryInit(this, launcherBinaries);
        modulesManager.invokeEvent(event);
        for(var e : launcherBinaries.values()) {
            e.init();
        }
    }

    public synchronized void buildLauncherBinaries() throws IOException {
        List<PipelineContext> contexts = new ArrayList<>(2);
        try {
            List<UpdatesProvider.UpdateUploadInfo> list = new ArrayList<>(2);
            var mainJar = launcherBinaries.get(CoreFeatureAPI.UpdateVariant.JAR);
            { // Always build JAR first
                var context = mainJar.build();
                var info = context.makeUploadInfo(CoreFeatureAPI.UpdateVariant.JAR);
                list.add(info);
                contexts.add(context);
            }
            for(var e : launcherBinaries.entrySet()) {
                if(e.getValue() == mainJar) {
                    continue;
                }
                var variant = e.getKey();
                var binary = e.getValue();
                var context = binary.build();
                var info = context.makeUploadInfo(variant);
                list.add(info);
                contexts.add(context);
            }
            config.updatesProvider.pushUpdate(list);
        } finally {
            for(var e : contexts) {
                e.clear();
            }
        }
    }

    @Override
    public synchronized void close() throws Exception {
        if (!closed.compareAndSet(false, true)) return;
        commandHandler.requestStop();
        Throwable failure = null;
        service.shutdownNow();
        try { config.close(ReloadType.FULL); } catch (Throwable e) { failure = e; }
        try { mirrorManager.close(); } catch (Throwable e) {
            if (failure == null) failure = e; else failure.addSuppressed(e);
        }
        try { modulesManager.invokeEvent(new ClosePhase()); } catch (Throwable e) {
            if (failure == null) failure = e; else failure.addSuppressed(e);
        }
        try { launcherModuleLoader.close(); } catch (Throwable e) {
            if (failure == null) failure = e; else failure.addSuppressed(e);
        }
        try { modulesManager.close(); } catch (Throwable e) {
            if (failure == null) failure = e; else failure.addSuppressed(e);
        }
        termination.countDown();
        logger.info("LaunchServer stopped");
        if (failure instanceof Exception exception) throw exception;
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new IllegalStateException("LaunchServer shutdown failed", failure);
    }

    public synchronized void start() {
        if (closed.get()) throw new IllegalStateException("LaunchServer is closed");
        if (!started.compareAndSet(false, true)) throw new IllegalStateException("LaunchServer has already started");
        try {
            modulesManager.invokeEvent(new LaunchServerFullInitEvent(this));
            logger.info("LaunchServer started");
        } catch (RuntimeException | Error failure) {
            try { close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    @Override
    public void run() {
        start();
    }

    public void awaitTermination() throws InterruptedException {
        termination.await();
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

        void writeConfig(LaunchServerConfig config) throws IOException;

        LauncherModulesConfig readModulesConfig() throws IOException;

        void writeModulesConfig(LauncherModulesConfig config) throws IOException;
    }

    public static class LaunchServerDirectories {
        public static final String UPDATES_NAME = "updates",
                TRUSTSTORE_NAME = "truststore", LAUNCHERLIBRARIES_NAME = "launcher-libraries",
                LAUNCHERLIBRARIESCOMPILE_NAME = "launcher-libraries-compile", LAUNCHERPACK_NAME = "launcher-pack",
                KEY_NAME = ".keys", MODULES = "modules", LAUNCHER_MODULES = "launcher-modules",
                LIBRARIES = "libraries", CONTROL_FILE = "control-file", PROGUARD_DIR = "proguard-libraries";
        public Path updatesDir;
        public Path librariesDir;
        public Path launcherLibrariesDir;
        public Path launcherLibrariesCompileDir;
        public Path launcherPackDir;
        public Path keyDirectory;
        public Path proguardDir;
        public Path dir;
        public Path trustStore;
        public Path tmpDir;
        public Path modules;
        public Path controlFile;

        public void collect() {
            if (updatesDir == null) updatesDir = getPath(UPDATES_NAME);
            if (trustStore == null) trustStore = getPath(TRUSTSTORE_NAME);
            if (launcherLibrariesDir == null) launcherLibrariesDir = getStaticPath(LAUNCHERLIBRARIES_NAME);
            if (launcherLibrariesCompileDir == null)
                launcherLibrariesCompileDir = getStaticPath(LAUNCHERLIBRARIESCOMPILE_NAME);
            if (launcherPackDir == null)
                launcherPackDir = getPath(LAUNCHERPACK_NAME);
            if (keyDirectory == null) keyDirectory = getPath(KEY_NAME);
            if (modules == null) modules = getStaticPath(MODULES);
            if (librariesDir == null) librariesDir = getStaticPath(LIBRARIES);
            if (controlFile == null) controlFile = getPath(CONTROL_FILE);
            if (proguardDir == null) proguardDir = getStaticPath(PROGUARD_DIR);
            if (tmpDir == null)
                tmpDir = Paths.get(System.getProperty("java.io.tmpdir")).resolve("launchserver-%s".formatted(SecurityHelper.randomStringToken()));
        }

        private Path getPath(String dirName) {
            String property = System.getProperty("launchserver.dir." + dirName, null);
            if (property == null) return dir.resolve(dirName);
            else return Paths.get(property);
        }

        private Path getStaticPath(String dirName) {
            String property = System.getProperty("launchserver.dir." + dirName, null);
            if (property == null) {
                String appHome = System.getenv("APP_HOME");
                if(appHome != null) {
                    return Path.of(appHome).resolve(dirName);
                } else {
                    return dir.resolve(dirName);
                }
            }
            else return Paths.get(property);
        }
    }
}
