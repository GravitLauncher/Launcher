package ru.gravit.launcher.server;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.auth.AuthServerRequest;
import ru.gravit.launcher.request.update.ProfilesRequest;
import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class ServerWrapper {
    public static ModulesManager modulesManager;
    public static Config config;
    public static PublicURLClassLoader ucp;
    public static ClassLoader loader;
    private static Gson gson;
    private static GsonBuilder gsonBuiler;

    public static Path modulesDir = Paths.get(System.getProperty("serverwrapper.modulesDir", "modules"));
    public static Path configFile = Paths.get(System.getProperty("serverwrapper.configFile", "ServerWrapperConfig.json"));
    public static Path publicKeyFile = Paths.get(System.getProperty("serverwrapper.publicKeyFile", "public.key"));

    public static boolean auth(ServerWrapper wrapper) {
        try {
            LauncherConfig cfg = Launcher.getConfig();
            Boolean auth = new AuthServerRequest(cfg, config.login, SecurityHelper.newRSAEncryptCipher(cfg.publicKey).doFinal(IOHelper.encode(config.password)), 0, config.title).request();
            if (auth == null) throw new Exception("Non auth!"); // security check 
            ProfilesRequest.Result result = new ProfilesRequest(cfg).request();
            for (ClientProfile p : result.profiles) {
                LogHelper.debug("Get profile: %s", p.getTitle());
                if (p.getTitle().equals(config.title)) {
                    wrapper.profile = p;
                    Launcher.profile = p;
                    LogHelper.debug("Found profile: %s", Launcher.profile.getTitle());
                    break;
                }
            }
            return true;
        } catch (Throwable e) {
            LogHelper.error(e);
            return false;
        }

    }

    public static boolean loopAuth(ServerWrapper wrapper, int count, int sleeptime) {
        if (count == 0) {
            while (true) {
                if (auth(wrapper)) return true;
            }
        }
        for (int i = 0; i < count; ++i) {
            if (auth(wrapper)) return true;
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public static void main(String[] args) throws Throwable {
        ServerWrapper wrapper = new ServerWrapper();
        LogHelper.printVersion("ServerWrapper");
        LogHelper.printLicense("ServerWrapper");
        modulesManager = new ModulesManager(wrapper);
        modulesManager.autoload(modulesDir);
        Launcher.modulesManager = modulesManager;
        modulesManager.preInitModules();
        LogHelper.debug("Read ServerWrapperConfig.json");
        gsonBuiler = new GsonBuilder();
        gsonBuiler.setPrettyPrinting();
        gson = gsonBuiler.create();
        generateConfigIfNotExists();
        try(Reader reader = IOHelper.newReader(configFile))
        {
            config = gson.fromJson(reader,Config.class);
        }
        LauncherConfig cfg = new LauncherConfig(config.address, config.port, SecurityHelper.toPublicRSAKey(IOHelper.read(publicKeyFile)), new HashMap<>(), config.projectname);
        Launcher.setConfig(cfg);
        if (config.syncAuth) auth(wrapper);
        else
            CommonHelper.newThread("Server Auth Thread", true, () -> ServerWrapper.loopAuth(wrapper, config.reconnectCount, config.reconnectSleep));
        modulesManager.initModules();
        String classname = (config.mainclass == null || config.mainclass.isEmpty()) ? args[0] : config.mainclass;
        if (classname.length() == 0) {
            LogHelper.error("MainClass not found. Please set MainClass for ServerWrapper.cfg or first commandline argument");
        }
        Class<?> mainClass;
        if (config.customClassPath) {
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
            Path librariesDir = Paths.get(config.librariesDir);
            LogHelper.info("Load libraries");
            ServerAgent.loadLibraries(librariesDir);
        }
        if (loader != null) mainClass = Class.forName(classname, true, loader);
        else mainClass = Class.forName(classname);
        MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        String[] real_args = new String[args.length - 1];
        System.arraycopy(args, 1, real_args, 0, args.length - 1);
        modulesManager.postInitModules();
        LogHelper.info("ServerWrapper: Project %s, LaunchServer address: %s port %d. Title: %s", config.projectname, config.address, config.port, config.title);
        LogHelper.info("Minecraft Version (for profile): %s", wrapper.profile == null ? "unknown" : wrapper.profile.getVersion().name);
        LogHelper.info("Start Minecraft Server");
        LogHelper.debug("Invoke main method %s", mainClass.getName());
        mainMethod.invoke(real_args);
    }

    private static void generateConfigIfNotExists() throws IOException {
        if (IOHelper.isFile(configFile))
            return;

        // Create new config
        LogHelper.info("Creating ServerWrapper config");
        Config newConfig= new Config();
        newConfig.title = "Your profile title";
        newConfig.projectname = "MineCraft";
        newConfig.address = "localhost";
        newConfig.port = 7240;
        newConfig.login = "login";
        newConfig.password = "password";
        newConfig.mainclass = "";
        //try(Reader reader = IOHelper.newReader(IOHelper.getResourceURL("ru/gravit/launcher/server/ServerWrapper.cfg")))
        //{
        //    newConfig = gson.fromJson(reader,Config.class);
        //}

        LogHelper.warning("Title is not set. Please show ServerWrapper.cfg");

        // Write LaunchServer config
        LogHelper.info("Writing ServerWrapper config file");
        try(Writer writer = IOHelper.newWriter(configFile))
        {
            gson.toJson(newConfig,writer);
        }
    }

    public static final class Config {
        public String title;
        public String projectname;
        public String address;
        public int port;
        public int reconnectCount;
        public int reconnectSleep;
        public boolean customClassPath;
        public boolean autoloadLibraries;
        public boolean syncAuth;
        public String classpath;
        public String librariesDir;
        public String mainclass;
        public String login;
        public String password;
    }

    public ClientProfile profile;
}
