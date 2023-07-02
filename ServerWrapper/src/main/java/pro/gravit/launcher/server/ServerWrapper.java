package pro.gravit.launcher.server;

import com.google.gson.GsonBuilder;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.api.KeyService;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.managers.GsonManager;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;
import pro.gravit.launcher.request.websockets.StdWebSocketService;
import pro.gravit.launcher.server.commands.InstallAuthLib;
import pro.gravit.launcher.server.commands.SetupCommand;
import pro.gravit.launcher.server.launch.ClasspathLaunch;
import pro.gravit.launcher.server.launch.Launch;
import pro.gravit.launcher.server.launch.ModuleLaunch;
import pro.gravit.launcher.server.launch.SimpleLaunch;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.command.JLineCommandHandler;
import pro.gravit.utils.command.StdCommandHandler;
import pro.gravit.utils.command.basic.HelpCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ServerWrapper extends JsonConfigurable<ServerWrapper.Config> {
    public static final Path configFile = Paths.get(System.getProperty("serverwrapper.configFile", "ServerWrapperConfig.json"));
    public static final boolean disableSetup = Boolean.parseBoolean(System.getProperty("serverwrapper.disableSetup", "false"));
    public CommandHandler commandHandler;
    public Config config;

    @Deprecated
    public ServerWrapper(Type type, Path configPath) {
        super(type, configPath);
    }

    public static void main(String... args) throws Throwable {
        LogHelper.printVersion("ServerWrapper");
        LogHelper.printLicense("ServerWrapper");

        Launcher.gsonManager = new GsonManager() {
            @Override
            public void registerAdapters(GsonBuilder builder) {
                super.registerAdapters(builder);
                ClientWebSocketService.appendTypeAdapters(builder);
            }
        };
        Launcher.gsonManager.initGson();

        new ServerWrapper(args);
    }

    public ServerWrapper(String[] args) throws Exception {
        super(ServerWrapper.Config.class, configFile);

        LogHelper.debug("Read ServerWrapperConfig.json");
        loadConfig();

        Launcher.applyLauncherEnv(config.env == null ? LauncherConfig.LauncherEnvironment.STD : config.env);

        if (config.logFile != null)
            LogHelper.addOutput(IOHelper.newWriter(Paths.get(config.logFile), true));

        AuthRequest.registerProviders();
        GetAvailabilityAuthRequest.registerProviders();
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();

        if (args.length > 0 && !disableSetup) {
            try {
                Class.forName("org.jline.terminal.Terminal");

                this.commandHandler = new JLineCommandHandler();
                LogHelper.debug("JLine2 terminal enabled");
            } catch (ClassNotFoundException ignored) {
                this.commandHandler = new StdCommandHandler(true);
                LogHelper.debug("JLine2 isn't in classpath, using std");
            }

            this.commandHandler.registerCommand("help", new HelpCommand(this.commandHandler));
            this.commandHandler.registerCommand("setup", new SetupCommand(this));
            this.commandHandler.registerCommand("installauthlib", new InstallAuthLib());
        }

        if (this.commandHandler != null) {
            Command command = this.commandHandler.findCommand(args[0].toLowerCase());

            if (command != null) {
                command.invoke(Arrays.copyOfRange(args, 1, args.length));

                System.exit(0);
            }
        }

        StdWebSocketService service = StdWebSocketService.initWebSockets(config.address).get();
        service.reconnectCallback = () -> {
            LogHelper.debug("WebSocket connect closed. Try reconnect");

            try {
                Request.reconnect();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        };

        Request.setRequestService(service);

        restore();

        if (config.encodedServerRsaPublicKey != null)
            KeyService.serverRsaPublicKey = SecurityHelper.toPublicRSAKey(config.encodedServerRsaPublicKey);

        String classname = (config.mainclass == null || config.mainclass.isEmpty()) ? args[0] : config.mainclass;
        if (classname.length() == 0) {
            LogHelper.error("MainClass not found. Please set MainClass for ServerWrapper.json or first commandline argument");
            System.exit(-1);
        }

        if (config.oauth == null && (config.extendedTokens == null || config.extendedTokens.isEmpty())) {
            LogHelper.error("Auth not configured. Please use 'java -jar ServerWrapper.jar setup'");
            System.exit(-1);
        }

        if (config.autoloadLibraries) {
            if (!ServerAgent.isAgentStarted)
                throw new UnsupportedOperationException("JavaAgent not found, autoLoadLibraries not available");

            if (config.librariesDir == null)
                throw new UnsupportedOperationException("librariesDir is null, autoLoadLibraries not available");

            LogHelper.info("Loading libraries");
            ServerAgent.loadLibraries(Paths.get(config.librariesDir));
        }

        LogHelper.info("Start Minecraft Server");

        List<String> real_args = Arrays.asList(args);
        if (config.args != null) {
            real_args.addAll(config.args);
        }

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

        LogHelper.debug("Invoke main method %s with %s", classname, launch.getClass().getCanonicalName());

        try {
            launch.run(classname, config, real_args.toArray(new String[0]));
        } catch (Throwable e) {
            LogHelper.error(e);
            System.exit(-1);
        }
    }

    public void restore() throws Exception {
        if (config.oauth != null)
            Request.setOAuth(config.authId, config.oauth, config.oauthExpireTime);

        if (config.extendedTokens != null)
            Request.addAllExtendedToken(config.extendedTokens);

        Request.restore();
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
        public String address;
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

        public byte[] encodedServerRsaPublicKey;

        public byte[] encodedServerEcPublicKey;
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
