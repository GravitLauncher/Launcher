package pro.gravit.launcher.server;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.api.AuthService;
import pro.gravit.launcher.base.api.ClientService;
import pro.gravit.launcher.base.api.ConfigService;
import pro.gravit.launcher.base.api.KeyService;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launcher.base.request.update.ProfilesRequest;
import pro.gravit.launcher.base.request.websockets.StdWebSocketService;
import pro.gravit.launcher.server.authlib.InstallAuthlib;
import pro.gravit.launcher.server.setup.ServerWrapperSetup;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.launch.*;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ServerWrapper extends JsonConfigurable<ServerWrapper.Config> {
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
        LogHelper.printVersion("ServerWrapper");
        LogHelper.printLicense("ServerWrapper");
        ServerWrapper.wrapper = new ServerWrapper(ServerWrapper.Config.class, configFile);
        ServerWrapper.wrapper.run(args);
    }

    public void restore() throws Exception {
        if(config.oauth != null) {
            Request.setOAuth(config.authId, config.oauth, config.oauthExpireTime);
        }
        if(config.extendedTokens != null) {
            Request.addAllExtendedToken(config.extendedTokens);
        }
        Request.RequestRestoreReport report = Request.restore(config.oauth != null, false, false);
        if(report.userInfo != null) {
            if(report.userInfo.playerProfile != null) {
                AuthService.username = report.userInfo.playerProfile.username;
                AuthService.uuid = report.userInfo.playerProfile.uuid;
            }
            AuthService.permissions = report.userInfo.permissions;
        }
    }

    public void getProfiles() throws Exception {
        ProfilesRequestEvent result = new ProfilesRequest().request();
        for (ClientProfile p : result.profiles) {
            LogHelper.debug("Get profile: %s", p.getTitle());
            boolean isFound = false;
            for (ClientProfile.ServerProfile srv : p.getServers()) {
                if (srv != null && srv.name.equals(config.serverName)) {
                    this.serverProfile = srv;
                    this.profile = p;
                    Launcher.profile = p;
                    AuthService.profile = p;
                    LogHelper.debug("Found profile: %s", Launcher.profile.getTitle());
                    isFound = true;
                    break;
                }
            }
            if (isFound) break;
        }
        if (profile == null) {
            LogHelper.warning("Not connected to ServerProfile. May be serverName incorrect?");
        }
    }

    public void run(String... args) throws Throwable {
        initGson();
        AuthRequest.registerProviders();
        GetAvailabilityAuthRequest.registerProviders();
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();
        if (args.length > 0 && args[0].equalsIgnoreCase("setup") && !disableSetup) {
            LogHelper.debug("Read ServerWrapperConfig.json");
            loadConfig();
            ServerWrapperSetup setup = new ServerWrapperSetup();
            setup.run();
            System.exit(0);
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("installAuthlib") && !disableSetup) {
            LogHelper.debug("Read ServerWrapperConfig.json");
            loadConfig();
            InstallAuthlib command = new InstallAuthlib();
            command. run(args[1]);
            System.exit(0);
        }
        LogHelper.debug("Read ServerWrapperConfig.json");
        loadConfig();
        updateLauncherConfig();
        Launcher.applyLauncherEnv(Objects.requireNonNullElse(config.env, LauncherConfig.LauncherEnvironment.STD));
        StdWebSocketService service = StdWebSocketService.initWebSockets(config.address).get();
        service.reconnectCallback = () ->
        {
            LogHelper.debug("WebSocket connect closed. Try reconnect");
            try {
                Request.reconnect();
                getProfiles();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        };
        if(config.properties != null) {
            for(Map.Entry<String, String> e : config.properties.entrySet()) {
                System.setProperty(e.getKey(), e.getValue());
            }
        }
        Request.setRequestService(service);
        if (config.logFile != null) LogHelper.addOutput(IOHelper.newWriter(Paths.get(config.logFile), true));
        {
            restore();
            getProfiles();
        }
        if(config.encodedServerRsaPublicKey != null) {
            KeyService.serverRsaPublicKey = SecurityHelper.toPublicRSAKey(config.encodedServerRsaPublicKey);
        }
        if(config.encodedServerEcPublicKey != null) {
            KeyService.serverEcPublicKey = SecurityHelper.toPublicECDSAKey(config.encodedServerEcPublicKey);
        }
        String classname = (config.mainclass == null || config.mainclass.isEmpty()) ? args[0] : config.mainclass;
        if (classname.isEmpty()) {
            LogHelper.error("MainClass not found. Please set MainClass for ServerWrapper.json or first commandline argument");
            System.exit(-1);
        }
        if(config.oauth == null && ( config.extendedTokens == null || config.extendedTokens.isEmpty())) {
            LogHelper.error("Auth not configured. Please use 'java -jar ServerWrapper.jar setup'");
            System.exit(-1);
        }
        if (config.autoloadLibraries) {
            if (!ServerAgent.isAgentStarted()) {
                throw new UnsupportedOperationException("JavaAgent not found, autoloadLibraries not available");
            }
            if (config.librariesDir == null)
                throw new UnsupportedOperationException("librariesDir is null, autoloadLibraries not available");
            Path librariesDir = Paths.get(config.librariesDir);
            LogHelper.info("Load libraries");
            ServerAgent.loadLibraries(librariesDir);
        }
        LogHelper.info("ServerWrapper: LaunchServer address: %s. Title: %s", config.address, Launcher.profile != null ? Launcher.profile.getTitle() : "unknown");
        LogHelper.info("Minecraft Version (for profile): %s", wrapper.profile == null ? "unknown" : wrapper.profile.getVersion().toString());
        String[] real_args;
        if(config.args != null && !config.args.isEmpty()) {
            real_args = config.args.toArray(new String[0]);
        } else if (args.length > 0) {
            real_args = new String[args.length - 1];
            System.arraycopy(args, 1, real_args, 0, args.length - 1);
        } else real_args = args;
        Launch launch;
        ClientService.nativePath = config.nativesDir;
        ConfigService.serverName = config.serverName;
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
                    launch = new BasicLaunch(ServerAgent.inst);
                } else {
                    launch = new BasicLaunch();
                }
                break;
        }
        LaunchOptions options = new LaunchOptions();
        options.enableHacks = config.enableHacks;
        options.moduleConf = config.moduleConf;
        classLoaderControl = launch.init(config.classpath.stream().map(Paths::get).collect(Collectors.toCollection(ArrayList::new)), config.nativesDir, options);
        if(ServerAgent.isAgentStarted()) {
            ClientService.instrumentation = ServerAgent.inst;
        }
        ClientService.classLoaderControl = classLoaderControl;
        ClientService.baseURLs = classLoaderControl.getURLs();
        if(config.configServiceSettings != null) {
            config.configServiceSettings.apply();
        }
        LogHelper.info("Start Minecraft Server");
        try {
            if(config.compatClasses != null) {
                for (String e : config.compatClasses) {
                    Class<?> clazz = classLoaderControl.getClass(e);
                    MethodHandle runMethod = MethodHandles.lookup().findStatic(clazz, "run", MethodType.methodType(void.class, ClassLoaderControl.class));
                    runMethod.invoke(classLoaderControl);
                }
            }
            LogHelper.debug("Invoke main method %s with %s", classname, launch.getClass().getName());
            launch.launch(config.mainclass, config.mainmodule, Arrays.asList(real_args));
        } catch (Throwable e) {
            LogHelper.error(e);
            System.exit(-1);
        }
    }

    public void updateLauncherConfig() {
        LauncherConfig cfg = new LauncherConfig(config.address, null, null, new HashMap<>(), "ServerWrapper");
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
        newConfig.serverName = "your server name";
        newConfig.mainclass = "";
        newConfig.extendedTokens = new HashMap<>();
        newConfig.args = new ArrayList<>();
        newConfig.classpath = new ArrayList<>();
        newConfig.address = "ws://localhost:9274/api";
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
        public String logFile;
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
        public AuthRequestEvent.OAuthRequestEvent oauth;
        public long oauthExpireTime;
        public Map<String, Request.ExtendedToken> extendedTokens;
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
    }
}
