package ru.gravit.launcher.server;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;
import ru.gravit.launcher.request.auth.AuthServerRequest;
import ru.gravit.launcher.serialize.config.ConfigObject;
import ru.gravit.launcher.serialize.config.TextConfigReader;
import ru.gravit.launcher.serialize.config.TextConfigWriter;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.BooleanConfigEntry;
import ru.gravit.launcher.serialize.config.entry.IntegerConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.update.ProfilesRequest;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.utils.helper.SecurityHelper;

public class ServerWrapper {
    public static ModulesManager modulesManager;
    public static Path configFile;
    public static Config config;
    public static boolean auth(ServerWrapper wrapper) {
        try {
            LauncherConfig cfg = Launcher.getConfig();
            Boolean auth = new AuthServerRequest(cfg,config.login,SecurityHelper.newRSAEncryptCipher(cfg.publicKey).doFinal(IOHelper.encode(config.password)),0,config.title).request();
            ProfilesRequest.Result result = new ProfilesRequest(cfg).request();
            for (SignedObjectHolder<ClientProfile> p : result.profiles) {
                LogHelper.debug("Get profile: %s", p.object.getTitle());
                if (p.object.getTitle().equals(config.title)) {
                    wrapper.profile = p.object;
                    Launcher.profile = p.object;
                    LogHelper.debug("Found profile: %s", Launcher.profile.getTitle());
                    break;
                }
            }
            return true;
        } catch (Throwable e)
        {
            LogHelper.error(e);
            return false;
        }

    }
    public static boolean loopAuth(ServerWrapper wrapper,int count,int sleeptime) {
        if(count == 0) {
            while(true) {
                if(auth(wrapper)) return true;
            }
        }
        for(int i=0;i<count;++i) {
            if(auth(wrapper)) return true;
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }
    public static void main(String[] args) throws Throwable {
        ServerWrapper wrapper = new ServerWrapper();
        modulesManager = new ModulesManager(wrapper);
        modulesManager.autoload(Paths.get("srv_modules")); //BungeeCord using modules dir
        Launcher.modulesManager = modulesManager;
        configFile = Paths.get("ServerWrapper.cfg");
        modulesManager.preInitModules();
        generateConfigIfNotExists();
        try (BufferedReader reader = IOHelper.newReader(configFile)) {
            config = new Config(TextConfigReader.read(reader, true));
        }
        LauncherConfig cfg = new LauncherConfig(config.address, config.port, SecurityHelper.toPublicRSAKey(IOHelper.read(Paths.get("public.key"))),new HashMap<>(),config.projectname);
        Launcher.setConfig(cfg);
        if(config.syncAuth) auth(wrapper);
        else CommonHelper.newThread("Server Auth Thread",true,() -> ServerWrapper.loopAuth(wrapper,config.reconnectCount,config.reconnectSleep));
        modulesManager.initModules();
        String classname = config.mainclass.isEmpty() ? args[0] : config.mainclass;
        Class<?> mainClass;
        if(config.customClassLoader) {
            @SuppressWarnings("unchecked")
			Class<ClassLoader> classloader_class = (Class<ClassLoader>) Class.forName(config.classloader);
            ClassLoader loader = classloader_class.getConstructor(ClassLoader.class).newInstance(ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(loader);
            mainClass = Class.forName(classname,false,loader);
        }
        else mainClass = Class.forName(classname);
        MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        String[] real_args = new String[args.length - 1];
        System.arraycopy(args, 1, real_args, 0, args.length - 1);
        modulesManager.postInitModules();
        LogHelper.debug("Invoke main method");
        mainMethod.invoke(real_args);
    }
    private static void generateConfigIfNotExists() throws IOException {
        if (IOHelper.isFile(configFile))
            return;

        // Create new config
        LogHelper.info("Creating LaunchWrapper config");
        Config newConfig;
        try (BufferedReader reader = IOHelper.newReader(IOHelper.getResourceURL("ru/gravit/launcher/server/ServerWrapper.cfg"))) {
            newConfig = new Config(TextConfigReader.read(reader, false));
        }

        LogHelper.warning("Title is not set. Please show ServerWrapper.cfg");

        // Write LaunchServer config
        LogHelper.info("Writing LaunchWrapper config file");
        try (BufferedWriter writer = IOHelper.newWriter(configFile)) {
            TextConfigWriter.write(newConfig.block, writer, true);
        }
    }
    public static final class Config extends ConfigObject {
        public String title;
        public String projectname;
        public String address;
        public int port;
        public int reconnectCount;
        public int reconnectSleep;
        public boolean customClassLoader;
        public boolean syncAuth;
        public String classloader;
        public String mainclass;
        public String login;
        public String password;
        protected Config(BlockConfigEntry block) {
            super(block);
            title = block.getEntryValue("title",StringConfigEntry.class);
            address = block.getEntryValue("address",StringConfigEntry.class);
            projectname = block.getEntryValue("projectName",StringConfigEntry.class);
            login = block.getEntryValue("login",StringConfigEntry.class);
            password = block.getEntryValue("password",StringConfigEntry.class);
            port = block.getEntryValue("port", IntegerConfigEntry.class);
            customClassLoader = block.getEntryValue("customClassLoader", BooleanConfigEntry.class);
            if(customClassLoader)
                classloader = block.getEntryValue("classloader",StringConfigEntry.class);
            mainclass = block.getEntryValue("MainClass",StringConfigEntry.class);
            reconnectCount = block.hasEntry("reconnectCount") ? block.getEntryValue("reconnectCount",IntegerConfigEntry.class) : 1;
            reconnectSleep = block.hasEntry("reconnectSleep") ? block.getEntryValue("reconnectSleep",IntegerConfigEntry.class) : 30000;
            syncAuth = block.hasEntry("syncAuth") ? block.getEntryValue("syncAuth",BooleanConfigEntry.class) : true;
        }
    }
    public ClientProfile profile;
}
