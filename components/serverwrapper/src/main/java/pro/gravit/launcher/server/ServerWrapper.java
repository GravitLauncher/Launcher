package pro.gravit.launcher.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.ApiMode;
import pro.gravit.launcher.base.api.AuthService;
import pro.gravit.launcher.base.api.ClientService;
import pro.gravit.launcher.base.api.ConfigService;
import pro.gravit.launcher.base.api.KeyService;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.client.LauncherAPIInitializer;
import pro.gravit.launcher.server.authlib.InstallAuthlib;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.FormatHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.launch.*;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerWrapper extends JsonConfigurable<ServerWrapper.Config> {

    private static final Logger logger =
            LoggerFactory.getLogger(ServerWrapper.class);

    public static final Path configFile = Paths.get(System.getProperty("serverwrapper.configFile", "ServerWrapperConfig.json"));
    public static final boolean disableSetup = Boolean.parseBoolean(System.getProperty("serverwrapper.disableSetup", "false"));
    public static ServerWrapper wrapper;
    public static ClassLoaderControl classLoaderControl;
    public Config config;
    public ClientProfile profile;
    public ClientProfile.ServerProfile serverProfile;

    public ServerWrapper(Type type, Path configPath) {
        super(type, configPath);
    }

    public static void initGson() {
        Launcher.gsonManager = new ServerWrapperGsonManager();
        Launcher.gsonManager.initGson();
    }

    public static void main(String... args) throws Throwable {
        logger.info(FormatHelper.formatVersion("ServerWrapper"));
        logger.info(FormatHelper.formatLicense("ServerWrapper"));
        ServerWrapper.wrapper = new ServerWrapper(ServerWrapper.Config.class, configFile);
        ServerWrapper.wrapper.run(args);
    }

    public void restore() throws Exception {
        if(config.accessToken != null) {
            var selfUser = LauncherAPIHolder.auth().restore(config.accessToken, true).get();
            if(selfUser != null) {
                AuthService.uuid = selfUser.getUUID();
                AuthService.username = selfUser.getUsername();
                if(selfUser.getPermissions() instanceof ClientPermissions clientPermissions) {
                    AuthService.permissions = clientPermissions;
                }
            }
            return;
        }
    }

    public void getProfiles() throws Exception {
    }


    public void initialize() throws Exception {
        initGson();
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();
        logger.debug("Read ServerWrapperConfig.json");
        loadConfig();
    }

    @Override
    public void loadConfig(com.google.gson.Gson gson, Path path) throws java.io.IOException {
        if (generateConfigIfNotExists(path)) return;
        try (var reader = IOHelper.newReader(path)) {
            var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("microsoftClientSecret")) {
                throw new IllegalArgumentException("microsoftClientSecret was removed; Microsoft authentication uses a public client");
            }
            if (json.has("logFile")) {
                throw new IllegalArgumentException("logFile was removed; configure logging through the SLF4J provider");
            }
            Config value = gson.fromJson(json, Config.class);
            if (value == null || value.apiMode == null) {
                throw new IllegalArgumentException("ServerWrapper configuration and apiMode must be valid; supported modes: AUTO, HTTP, MICROSOFT, OFFLINE");
            }
            setConfig(value);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new java.io.IOException("Invalid ServerWrapper configuration at " + path + ": " + e.getMessage(), e);
        }
    }

    public void connect() throws Exception {
        config.applyEnv();
        updateLauncherConfig();
        LauncherAPIInitializer.initialize(null, Launcher.getConfig(), List.of());
        LauncherAPIHolder.changeAuthId(config.authId == null ? "std" : config.authId);

        {
            restore();
            //getProfiles();
        }
        if(config.encodedServerRsaPublicKey != null) {
            KeyService.serverRsaPublicKey = SecurityHelper.toPublicRSAKey(config.encodedServerRsaPublicKey);
        }
        if(config.encodedServerEcPublicKey != null) {
            KeyService.serverEcPublicKey = SecurityHelper.toPublicECDSAKey(config.encodedServerEcPublicKey);
        }
        ClientService.nativePath = config.nativesDir;
        ConfigService.serverName = config.serverName;
        if(config.configServiceSettings != null) {
            config.configServiceSettings.apply();
        }
    }

    public void runCompatClasses() throws Throwable {
        if(config.compatClasses != null) {
            for (String e : config.compatClasses) {
                Class<?> clazz = classLoaderControl == null ? Class.forName(e) : classLoaderControl.getClass(e);
                MethodHandle runMethod = MethodHandles.lookup().findStatic(clazz, "run", MethodType.methodType(void.class, ClassLoaderControl.class));
                runMethod.invoke(classLoaderControl);
            }
        }
    }

    public void run(String... args) throws Throwable {
        initialize();
        if (args.length > 2 && args[0].equalsIgnoreCase("bridge") && !disableSetup) {
            ServerWrapperBridge bridge = new ServerWrapperBridge();
            connect();
            bridge.run(args[1], args[2]);
            System.exit(0);
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("installAuthlib") && !disableSetup) {
            InstallAuthlib command = new InstallAuthlib();
            command.run(args[1]);
            System.exit(0);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("save") && !disableSetup) {
            config.applyEnv();
            saveConfig();
            System.exit(0);
        }
        connect();
        if(config.properties != null) {
            for(Map.Entry<String, String> e : config.properties.entrySet()) {
                System.setProperty(e.getKey(), e.getValue());
            }
        }
        if(config.encodedServerRsaPublicKey != null) {
            KeyService.serverRsaPublicKey = SecurityHelper.toPublicRSAKey(config.encodedServerRsaPublicKey);
        }
        if(config.encodedServerEcPublicKey != null) {
            KeyService.serverEcPublicKey = SecurityHelper.toPublicECDSAKey(config.encodedServerEcPublicKey);
        }
        String classname;
        if (config.mainclass != null && !config.mainclass.isEmpty()) {
            classname = config.mainclass;
        } else if (args.length > 0) {
            classname = args[0];
        } else {
            classname = "";
        }
        if (classname.isEmpty()) {
            logger.error("MainClass not found. Please set MainClass for ServerWrapper.json or first commandline argument");
            System.exit(-1);
        }
        if (config.autoloadLibraries) {
            if (!ServerAgent.isAgentStarted()) {
                throw new UnsupportedOperationException("JavaAgent not found, autoloadLibraries not available");
            }
            if (config.librariesDir == null)
                throw new UnsupportedOperationException("librariesDir is null, autoloadLibraries not available");
            Path librariesDir = Paths.get(config.librariesDir);
            logger.info("Load libraries");
            ServerAgent.loadLibraries(librariesDir);
        }
        logger.info("ServerWrapper: LaunchServer address: {}. Title: {}", config.address, Launcher.profile != null ? Launcher.profile.getTitle() : "unknown");
        logger.info("Minecraft Version (for profile): {}", wrapper.profile == null ? "unknown" : wrapper.profile.getVersion().toString());
        String[] real_args;
        if(config.args != null && !config.args.isEmpty()) {
            real_args = config.args.toArray(new String[0]);
        } else if (args.length > 0) {
            real_args = new String[args.length - 1];
            System.arraycopy(args, 1, real_args, 0, args.length - 1);
        } else real_args = args;
        Launch launch;
        if(config.loadNatives != null) {
            for(String e : config.loadNatives) {
                System.load(Paths.get(config.nativesDir).resolve(ClientService.findLibrary(e)).toAbsolutePath().toString());
            }
        }
        switch (config.classLoaderConfig) {
            case LAUNCHER:
                launch = new LegacyLaunch();
                System.setProperty("java.class.path", String.join(File.pathSeparator, config.classpath));
                break;
            case MODULE:
                launch = new ModuleLaunch();
                System.setProperty("java.class.path", String.join(File.pathSeparator, config.classpath));
                break;
            default:
                if(ServerAgent.isAgentStarted()) {
                    launch = new pro.gravit.utils.launch.BasicLaunch(ServerAgent.inst);
                } else {
                    launch = new BasicLaunch();
                }
                break;
        }
        LaunchOptions options = new LaunchOptions();
        options.enableHacks = config.enableHacks;
        options.moduleConf = config.moduleConf;
        classLoaderControl = launch.init(config.classpath.stream()
                .map(Paths::get)
                        .flatMap(p -> {
                            if(!Files.isDirectory(p)) {
                                return Stream.of(p);
                            }
                            try {
                                return Files.walk(p).filter(e -> e.getFileName().toString().endsWith(".jar"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .collect(Collectors.toCollection(ArrayList::new)), config.nativesDir, options);
        if(ServerAgent.isAgentStarted()) {
            ClientService.instrumentation = ServerAgent.inst;
        }
        ClientService.classLoaderControl = classLoaderControl;
        ClientService.baseURLs = classLoaderControl.getURLs();
        logger.info("Start Minecraft Server");
        try {
            runCompatClasses();
            logger.debug("Invoke main method {} with {}", classname, launch.getClass().getName());
            launch.launch(config.mainclass, config.mainmodule, Arrays.asList(real_args));
        } catch (Throwable e) {
            logger.error("", e);
            System.exit(-1);
        }
    }

    public void updateLauncherConfig() {
        LauncherConfig cfg = new LauncherConfig(config.address, null, null, new HashMap<>(), "ServerWrapper");
        cfg.launcherApiMode = config.apiMode == null ? null : config.apiMode.name();
        cfg.launcherApiOfflineOnConnectionFail = config.offlineOnConnectionFail;
        cfg.launcherApiMicrosoftEnabled = config.microsoftAuthEnabled;
        cfg.launcherApiMicrosoftClientId = config.microsoftClientId;
        Launcher.setConfig(cfg);
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public Config getDefaultConfig() {
        Config newConfig = new Config();
        newConfig.serverName = "";
        newConfig.mainclass = "";
        newConfig.args = new ArrayList<>();
        newConfig.classpath = new ArrayList<>();
        newConfig.address = "";
        newConfig.classLoaderConfig = ClientProfile.ClassLoaderConfig.SYSTEM_ARGS;
        newConfig.env = LauncherConfig.LauncherEnvironment.STD;
        newConfig.properties = new HashMap<>();
        return newConfig;
    }

    public static final class Config {
        @Deprecated
        public String projectname;
        public String address;
        public String serverName;
        public boolean autoloadLibraries;
        public List<String> classpath;
        public ClientProfile.ClassLoaderConfig classLoaderConfig;
        public String librariesDir;
        public String mainclass;
        public String mainmodule;
        public String nativesDir = "natives";
        public List<String> args;
        public List<String> compatClasses;
        public List<String> loadNatives;
        public String authId;
        public ApiMode apiMode = ApiMode.AUTO;
        public boolean offlineOnConnectionFail = true;
        public boolean microsoftAuthEnabled;
        public String microsoftClientId;
        public String accessToken;
        public long oauthExpireTime;
        public LauncherConfig.LauncherEnvironment env;
        public LaunchOptions.ModuleConf moduleConf = new LaunchOptions.ModuleConf();

        public byte[] encodedServerRsaPublicKey;

        public byte[] encodedServerEcPublicKey;
        public boolean enableHacks;

        public Map<String, String> properties;
        public ConfigServiceSettings configServiceSettings = new ConfigServiceSettings();

        public static class ConfigServiceSettings {
            public boolean disableLogging = false;
            public boolean checkServerNeedProperties = false;
            public boolean checkServerNeedHardware = false;
            public void apply() {
                ConfigService.disableLogging = disableLogging;
                ConfigService.checkServerConfig.needHardware = checkServerNeedHardware;
                ConfigService.checkServerConfig.needProperties = checkServerNeedProperties;
            }
        }

        public void applyEnv() {
            this.authId = applyEnvOrDefault("SERVERWRAPPER_AUTH_ID", this.authId);
            this.address = applyEnvOrDefault("SERVERWRAPPER_ADDRESS", this.address);
            this.serverName = applyEnvOrDefault("SERVERWRAPPER_SERVER_NAME", this.serverName);
            this.apiMode = applyEnvOrDefault("SERVERWRAPPER_API_MODE", ApiMode::valueOf, this.apiMode);
            this.offlineOnConnectionFail = applyEnvOrDefault("SERVERWRAPPER_OFFLINE_ON_CONNECTION_FAIL", Boolean::parseBoolean, this.offlineOnConnectionFail);
            this.microsoftAuthEnabled = applyEnvOrDefault("SERVERWRAPPER_MICROSOFT_AUTH_ENABLED", Boolean::parseBoolean, this.microsoftAuthEnabled);
            this.microsoftClientId = applyEnvOrDefault("SERVERWRAPPER_MICROSOFT_CLIENT_ID", this.microsoftClientId);
            this.encodedServerEcPublicKey = applyEnvOrDefault("SERVERWRAPPER_EC_PUBLIC_KEY", Base64.getUrlDecoder()::decode, null);
            this.encodedServerRsaPublicKey = applyEnvOrDefault("SERVERWRAPPER_RSA_PUBLIC_KEY", Base64.getUrlDecoder()::decode, null);
            {
                String accessToken = System.getenv("SERVERWRAPPER_ACCESS_TOKEN");
                if(accessToken != null) {
                    this.accessToken = accessToken;
                }
            }
        }

        private static String applyEnvOrDefault(String envName, String def) {
            String value = System.getenv(envName);
            if(value == null) {
                return def;
            }
            return value;
        }

        private static<T> T applyEnvOrDefault(String envName, Function<String, T> mappingFunction, T def) {
            String value = System.getenv(envName);
            if(value == null) {
                return def;
            }
            return mappingFunction.apply(value);
        }
    }
}
