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
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.update.ProfilesRequest;
import pro.gravit.launcher.server.setup.ServerWrapperSetup;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;

public class ServerWrapper extends JsonConfigurable<ServerWrapper.Config> {
    public static final Path modulesDir = Paths.get(System.getProperty("serverwrapper.modulesDir", "modules"));
    public static final Path modulesConfigDir = Paths.get(System.getProperty("serverwrapper.modulesConfigDir", "modules-config"));
    public static final Path configFile = Paths.get(System.getProperty("serverwrapper.configFile", "ServerWrapperConfig.json"));
    public static final Path publicKeyFile = Paths.get(System.getProperty("serverwrapper.publicKeyFile", "public.key"));
    public static final boolean disableSetup = Boolean.parseBoolean(System.getProperty("serverwrapper.disableSetup", "false"));
    public static ServerWrapperModulesManager modulesManager;
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

    public static void initGson(ServerWrapperModulesManager modulesManager) {
        Launcher.gsonManager = new ServerWrapperGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
    }

    public static void main(String... args) throws Throwable {
        LogHelper.printVersion("ServerWrapper");
        LogHelper.printLicense("ServerWrapper");
        modulesManager = new ServerWrapperModulesManager(modulesDir, modulesConfigDir);
        modulesManager.autoload();
        modulesManager.initModules(null);
        ServerWrapper.wrapper = new ServerWrapper(ServerWrapper.Config.class, configFile);
        ServerWrapper.wrapper.run(args);
    }

    public boolean auth() {
        try {
            Launcher.getConfig();
            AuthRequest request = new AuthRequest(config.login, config.password, config.auth_id, AuthRequest.ConnectTypes.API);
            AuthRequestEvent authResult = request.request();
            permissions = authResult.permissions;
            playerProfile = authResult.playerProfile;
            ProfilesRequestEvent result = new ProfilesRequest().request();
            for (ClientProfile p : result.profiles) {
                LogHelper.debug("Get profile: %s", p.getTitle());
                boolean isFound = false;
                for(ClientProfile.ServerProfile srv : p.getServers())
                {
                    if(srv != null && srv.name.equals(config.serverName)) {
                        this.serverProfile = srv;
                        this.profile = p;
                        Launcher.profile = p;
                        LogHelper.debug("Found profile: %s", Launcher.profile.getTitle());
                        isFound = true;
                        break;
                    }
                }
                if(isFound) break;
            }
            if (profile == null) {
                LogHelper.warning("Not connected to ServerProfile. May be serverName incorrect?");
            }
            return true;
        } catch (Throwable e) {
            LogHelper.error(e);
            if (config.stopOnError) System.exit(-1);
            return false;
        }

    }

    public boolean loopAuth(int count, int sleeptime) {
        if (count == 0) {
            while (true) {
                if (auth()) return true;
            }
        }
        for (int i = 0; i < count; ++i) {
            if (auth()) return true;
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LogHelper.error(e);
                return false;
            }
        }
        return false;
    }

    public void run(String... args) throws Throwable {
        initGson(modulesManager);
        AuthRequest.registerProviders();
        OptionalAction.registerProviders();
        if (args.length > 0 && args[0].equals("setup") && !disableSetup) {
            LogHelper.debug("Read ServerWrapperConfig.json");
            loadConfig();
            ServerWrapperSetup setup = new ServerWrapperSetup();
            setup.run();
            System.exit(0);
        }
        modulesManager.invokeEvent(new PreConfigPhase());
        LogHelper.debug("Read ServerWrapperConfig.json");
        loadConfig();
        updateLauncherConfig();
        if (config.env != null) Launcher.applyLauncherEnv(config.env);
        else Launcher.applyLauncherEnv(LauncherConfig.LauncherEnvironment.STD);
        if (config.logFile != null) LogHelper.addOutput(IOHelper.newWriter(Paths.get(config.logFile), true));
        if (config.syncAuth) auth();
        else
            CommonHelper.newThread("Server Auth Thread", true, () -> loopAuth(config.reconnectCount, config.reconnectSleep));
        modulesManager.invokeEvent(new ServerWrapperInitPhase(this));
        String classname = (config.mainclass == null || config.mainclass.isEmpty()) ? args[0] : config.mainclass;
        if (classname.length() == 0) {
            LogHelper.error("MainClass not found. Please set MainClass for ServerWrapper.cfg or first commandline argument");
            if (config.stopOnError) System.exit(-1);
        }
        Class<?> mainClass;
        if (config.customClassPath) {
            if (config.classpath == null)
                throw new UnsupportedOperationException("classpath is null, customClassPath not available");
            String[] cp = config.classpath.split(":");
            if (!ServerAgent.isAgentStarted()) {
                LogHelper.warning("JavaAgent not found. Using URLClassLoader");
                URL[] urls = Arrays.stream(cp).map(Paths::get).map(IOHelper::toURL).toArray(URL[]::new);
                ucp = new PublicURLClassLoader(urls);
                Thread.currentThread().setContextClassLoader(ucp);
                loader = ucp;
            } else {
                LogHelper.info("Found %d custom classpath elements", cp.length);
                for (String c : cp)
                    ServerAgent.addJVMClassPath(c);
            }
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
        if (loader != null) mainClass = Class.forName(classname, true, loader);
        else mainClass = Class.forName(classname);
        MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        modulesManager.invokeEvent(new PostInitPhase());
        Request.service.reconnectCallback = () ->
        {
            LogHelper.debug("WebSocket connect closed. Try reconnect");
            try {
                Request.service.open();
                LogHelper.debug("Connect to %s", config.address);
            } catch (Exception e) {
                LogHelper.error(e);
                throw new RequestException(String.format("Connect error: %s", e.getMessage() != null ? e.getMessage() : "null"));
            }
            auth();
        };
        LogHelper.info("ServerWrapper: Project %s, LaunchServer address: %s. Title: %s", config.projectname, config.address, Launcher.profile != null ? Launcher.profile.getTitle() : "unknown");
        LogHelper.info("Minecraft Version (for profile): %s", wrapper.profile == null ? "unknown" : wrapper.profile.getVersion().name);
        LogHelper.info("Start Minecraft Server");
        LogHelper.debug("Invoke main method %s", mainClass.getName());
        if (config.args == null) {
            String[] real_args;
            if (args.length > 0) {
                real_args = new String[args.length - 1];
                System.arraycopy(args, 1, real_args, 0, args.length - 1);
            } else real_args = args;

            mainMethod.invoke(real_args);
        } else {
            mainMethod.invoke(config.args);
        }
    }

    public void updateLauncherConfig() {

        LauncherConfig cfg = null;
        try {
            ECPublicKey publicKey = null;
            if (IOHelper.isFile(publicKeyFile))
                publicKey = SecurityHelper.toPublicECKey(IOHelper.read(publicKeyFile));
            cfg = new LauncherConfig(config.address, publicKey, new HashMap<>(), config.projectname);
            cfg.address = config.address;
        } catch (InvalidKeySpecException | IOException e) {
            LogHelper.error(e);
        }
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
        newConfig.projectname = "MineCraft";
        newConfig.login = "login";
        newConfig.password = "password";
        newConfig.mainclass = "";
        newConfig.syncAuth = true;
        newConfig.stopOnError = true;
        newConfig.reconnectCount = 10;
        newConfig.reconnectSleep = 1000;
        newConfig.websocket = new WebSocketConf();
        newConfig.address = "ws://localhost:9274/api";
        newConfig.env = LauncherConfig.LauncherEnvironment.STD;
        return newConfig;
    }

    public static final class Config {
        @Deprecated
        public String title;
        public String projectname;
        public String address;
        public String serverName;
        public WebSocketConf websocket;
        public int reconnectCount;
        public int reconnectSleep;
        public boolean customClassPath;
        public boolean autoloadLibraries;
        public boolean stopOnError;
        public boolean syncAuth;
        public String logFile;
        public String classpath;
        public String librariesDir;
        public String mainclass;
        public String login;
        public String[] args;
        public String password;
        public String auth_id = "";
        public LauncherConfig.LauncherEnvironment env;
    }

    public static final class WebSocketConf {
    }
}
