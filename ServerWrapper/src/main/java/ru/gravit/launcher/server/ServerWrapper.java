package ru.gravit.launcher.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.events.request.ProfilesRequestEvent;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.auth.AuthRequest;
import ru.gravit.launcher.request.update.ProfilesRequest;
import ru.gravit.launcher.request.websockets.LegacyRequestBridge;
import ru.gravit.launcher.server.setup.ServerWrapperSetup;
import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.utils.config.JsonConfigurable;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;

public class ServerWrapper extends JsonConfigurable<ServerWrapper.Config> {
    public ModulesManager modulesManager;
    public Config config;
    public PublicURLClassLoader ucp;
    public ClassLoader loader;
    public ClientPermissions permissions;
    public static ServerWrapper wrapper;
    public static Gson gson;
    private static GsonBuilder gsonBuiler;

    public static Path modulesDir = Paths.get(System.getProperty("serverwrapper.modulesDir", "modules"));
    public static Path configFile = Paths.get(System.getProperty("serverwrapper.configFile", "ServerWrapperConfig.json"));
    public static Path publicKeyFile = Paths.get(System.getProperty("serverwrapper.publicKeyFile", "public.key"));
    public static boolean disableSetup = Boolean.valueOf(System.getProperty("serverwrapper.disableSetup", "false"));

    public ServerWrapper(Type type, Path configPath) {
        super(type, configPath);
    }

    public boolean auth() {
        try {
            LauncherConfig cfg = Launcher.getConfig();
            AuthRequest request = new AuthRequest(config.login, SecurityHelper.newRSAEncryptCipher(cfg.publicKey).doFinal(IOHelper.encode(config.password)), config.auth_id, AuthRequest.ConnectTypes.SERVER);
            permissions = request.request().permissions;
            ProfilesRequestEvent result = new ProfilesRequest().request();
            for (ClientProfile p : result.profiles) {
                LogHelper.debug("Get profile: %s", p.getTitle());
                if (p.getTitle().equals(config.title)) {
                    profile = p;
                    Launcher.profile = p;
                    LogHelper.debug("Found profile: %s", Launcher.profile.getTitle());
                    break;
                }
            }
            if (profile == null) {
                LogHelper.error("Your profile not found");
                if (config.stopOnError) System.exit(-1);
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

    public static void initGson() {
        if (Launcher.gson != null) return;
        Launcher.gsonBuilder = new GsonBuilder();
        Launcher.gson = Launcher.gsonBuilder.create();
    }

    public void run(String... args) throws Throwable {
        gsonBuiler = new GsonBuilder();
        gsonBuiler.setPrettyPrinting();
        gson = gsonBuiler.create();
        initGson();
        if (args.length > 0 && args[0].equals("setup") && !disableSetup) {
            LogHelper.debug("Read ServerWrapperConfig.json");
            loadConfig();
            ServerWrapperSetup setup = new ServerWrapperSetup();
            setup.run();
            System.exit(0);
        }
        modulesManager = new ModulesManager(wrapper);
        modulesManager.autoload(modulesDir);
        Launcher.modulesManager = modulesManager;
        modulesManager.preInitModules();
        LogHelper.debug("Read ServerWrapperConfig.json");
        loadConfig();
        updateLauncherConfig();
        if (config.env != null) Launcher.applyLauncherEnv(config.env);
        else Launcher.applyLauncherEnv(LauncherConfig.LauncherEnvironment.STD);
        if (config.logFile != null) LogHelper.addOutput(IOHelper.newWriter(Paths.get(config.logFile), true));
        if (config.syncAuth) auth();
        else
            CommonHelper.newThread("Server Auth Thread", true, () -> loopAuth(config.reconnectCount, config.reconnectSleep));
        modulesManager.initModules();
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
        modulesManager.postInitModules();
        if(config.websocket.enabled)
        {
            LegacyRequestBridge.service.reconnectCallback = () ->
            {
                LogHelper.debug("WebSocket connect closed. Try reconnect");
                try {
                    if (!LegacyRequestBridge.service.reconnectBlocking()) LogHelper.error("Error connecting");
                    LogHelper.debug("Connect to %s", config.websocket.address);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                auth();
            };
        }
        LogHelper.info("ServerWrapper: Project %s, LaunchServer address: %s port %d. Title: %s", config.projectname, config.websocket.address, config.title);
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
            cfg = new LauncherConfig(config.websocket.address, SecurityHelper.toPublicRSAKey(IOHelper.read(publicKeyFile)), new HashMap<>(), config.projectname);
            if(config.websocket != null && config.websocket.enabled)
            {
                cfg.isNettyEnabled = true;
                cfg.address = config.websocket.address;
            }
        } catch (InvalidKeySpecException | IOException e) {
            LogHelper.error(e);
        }
        Launcher.setConfig(cfg);
    }

    public static void main(String... args) throws Throwable {
        LogHelper.printVersion("ServerWrapper");
        LogHelper.printLicense("ServerWrapper");
        ServerWrapper.wrapper = new ServerWrapper(ServerWrapper.Config.class, configFile);
        ServerWrapper.wrapper.run(args);
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public Config getDefaultConfig() {
        Config newConfig = new Config();
        newConfig.title = "Your profile title";
        newConfig.projectname = "MineCraft";
        newConfig.login = "login";
        newConfig.password = "password";
        newConfig.mainclass = "";
        newConfig.syncAuth = true;
        newConfig.stopOnError = true;
        newConfig.reconnectCount = 10;
        newConfig.reconnectSleep = 1000;
        newConfig.websocket = new WebSocketConf();
        newConfig.websocket.address = "ws://localhost:9274/api";
        newConfig.websocket.enabled = false;
        newConfig.env = LauncherConfig.LauncherEnvironment.STD;
        return newConfig;
    }

    @Override
    public void setConfig(Config config) {
        this.config = config;
    }

    public static final class Config {
        public String title;
        public String projectname;
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
    public static final class WebSocketConf
    {
        public boolean enabled;
        public String address;
    }

    public ClientProfile profile;
}
