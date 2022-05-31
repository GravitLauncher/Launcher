package pro.gravit.launcher.server;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.ProfilesRequestEvent;
import pro.gravit.launcher.modules.events.PostInitPhase;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launcher.request.auth.RestoreRequest;
import pro.gravit.launcher.request.update.ProfilesRequest;
import pro.gravit.launcher.request.websockets.StdWebSocketService;
import pro.gravit.launcher.server.launch.ClasspathLaunch;
import pro.gravit.launcher.server.launch.Launch;
import pro.gravit.launcher.server.launch.ModuleLaunch;
import pro.gravit.launcher.server.launch.SimpleLaunch;
import pro.gravit.launcher.server.setup.ServerWrapperSetup;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ServerWrapper extends JsonConfigurable<ServerWrapper.Config> {
    public static final Path configFile = Paths.get(System.getProperty("serverwrapper.configFile", "ServerWrapperConfig.json"));
    public static final boolean disableSetup = Boolean.parseBoolean(System.getProperty("serverwrapper.disableSetup", "false"));
    public static ServerWrapper wrapper;
    public Config config;
    public PublicURLClassLoader ucp;
    public ClassLoader loader;
    public ClientPermissions permissions;
    public ClientProfile profile;
    public PlayerProfile playerProfile;
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
        Request.restore();
    }

    public ProfilesRequestEvent getProfiles() throws Exception {
        ProfilesRequestEvent result = new ProfilesRequest().request();
        for (ClientProfile p : result.profiles) {
            LogHelper.debug("Get profile: %s", p.getTitle());
            boolean isFound = false;
            for (ClientProfile.ServerProfile srv : p.getServers()) {
                if (srv != null && srv.name.equals(config.serverName)) {
                    this.serverProfile = srv;
                    this.profile = p;
                    Launcher.profile = p;
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
        return result;
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void run(String... args) throws Throwable {
        initGson();
        AuthRequest.registerProviders();
        GetAvailabilityAuthRequest.registerProviders();
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();
        if (args.length > 0 && args[0].equals("setup") && !disableSetup) {
            LogHelper.debug("Read ServerWrapperConfig.json");
            loadConfig();
            ServerWrapperSetup setup = new ServerWrapperSetup();
            setup.run();
            System.exit(0);
        }
        LogHelper.debug("Read ServerWrapperConfig.json");
        loadConfig();
        updateLauncherConfig();
        if (config.env != null) Launcher.applyLauncherEnv(config.env);
        else Launcher.applyLauncherEnv(LauncherConfig.LauncherEnvironment.STD);
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
        Request.setRequestService(service);
        if (config.logFile != null) LogHelper.addOutput(IOHelper.newWriter(Paths.get(config.logFile), true));
        {
            restore();
            getProfiles();
        }
        String classname = (config.mainclass == null || config.mainclass.isEmpty()) ? args[0] : config.mainclass;
        if (classname.length() == 0) {
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
        LogHelper.info("Minecraft Version (for profile): %s", wrapper.profile == null ? "unknown" : wrapper.profile.getVersion().name);
        String[] real_args;
        if(config.args != null && config.args.size() > 0) {
            real_args = config.args.toArray(new String[0]);
        } else if (args.length > 0) {
            real_args = new String[args.length - 1];
            System.arraycopy(args, 1, real_args, 0, args.length - 1);
        } else real_args = args;
        Launch launch;
        switch (config.classLoaderConfig) {
            case LAUNCHER:
                launch = new ClasspathLaunch();
                break;
            case MODULE:
                launch = new ModuleLaunch();
                break;
            default:
                launch = new SimpleLaunch();
                break;
        }
        LogHelper.info("Start Minecraft Server");
        LogHelper.debug("Invoke main method %s with %s", config.mainclass, launch.getClass().getName());
        try {
            launch.run(config, real_args);
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
        public List<String> args;
        public String authId;
        public AuthRequestEvent.OAuthRequestEvent oauth;
        public long oauthExpireTime;
        public Map<String, String> extendedTokens;
        public LauncherConfig.LauncherEnvironment env;
        public ModuleConf moduleConf = new ModuleConf();
    }

    public static final class ModuleConf {
        public List<String> modules = new ArrayList<>();
        public List<String> modulePath = new ArrayList<>();
        public String mainModule = "";
        public Map<String, String> exports = new HashMap<>();
        public Map<String, String> opens = new HashMap<>();
        public Map<String, String> reads = new HashMap<>();
    }
}
