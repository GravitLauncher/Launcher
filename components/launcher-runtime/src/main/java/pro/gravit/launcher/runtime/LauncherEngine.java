package pro.gravit.launcher.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.request.*;
import pro.gravit.launcher.client.*;
import pro.gravit.launcher.core.api.LauncherAPI;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.*;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.runtime.backend.LauncherBackendImpl;
import pro.gravit.launcher.runtime.client.*;
import pro.gravit.launcher.runtime.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.runtime.client.events.ClientPreGuiPhase;
import pro.gravit.launcher.runtime.console.ModulesCommand;
import pro.gravit.launcher.runtime.gui.NoRuntimeProvider;
import pro.gravit.launcher.runtime.gui.RuntimeProvider;
import pro.gravit.launcher.runtime.managers.ConsoleManager;
import pro.gravit.launcher.client.utils.NativeJVMHalt;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.base.modules.events.OfflineModeEvent;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.base.request.auth.*;
import pro.gravit.launcher.base.request.websockets.OfflineRequestService;
import pro.gravit.launcher.base.request.websockets.StdWebSocketService;
import pro.gravit.launcher.start.RuntimeModuleManager;
import pro.gravit.utils.helper.*;

import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherEngine {

    private static final Logger logger =
            LoggerFactory.getLogger(LauncherEngine.class);

    public static ClientParams clientParams;
    public static RuntimeModuleManager modulesManager;
    public final boolean clientInstance;
    // Instance
    private final AtomicBoolean started = new AtomicBoolean(false);
    public RuntimeProvider runtimeProvider;
    public Class<? extends RuntimeProvider> basicRuntimeProvider;

    private LauncherEngine(boolean clientInstance, Class<? extends RuntimeProvider> basicRuntimeProvider) {
        this.clientInstance = clientInstance;
        this.basicRuntimeProvider = basicRuntimeProvider;
    }

    //JVMHelper.getCertificates
    public static X509Certificate[] getCertificates(Class<?> clazz) {
        Object[] signers = clazz.getSigners();
        if (signers == null) return null;
        return Arrays.stream(signers).filter((c) -> c instanceof X509Certificate).map((c) -> (X509Certificate) c).toArray(X509Certificate[]::new);
    }

    public static void checkClass(Class<?> clazz) throws SecurityException {
        LauncherTrustManager trustManager = Launcher.getConfig().trustManager;
        if (trustManager == null) return;
        X509Certificate[] certificates = getCertificates(clazz);
        if (certificates == null) {
            throw new SecurityException(String.format("Class %s not signed", clazz.getName()));
        }
        try {
            trustManager.checkCertificatesSuccess(certificates, trustManager::stdCertificateChecker);
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    public static void beforeExit(int code) {
        try {
            modulesManager.invokeEvent(new ClientExitPhase(code));
        } catch (Throwable ignored) {
        }
    }

    public static void forceExit(int code) {
        try {
            System.exit(code);
        } catch (Throwable e) //Forge Security Manager?
        {
            NativeJVMHalt.haltA(code);
        }
    }

    public static void exitLauncher(int code) {
        beforeExit(code);
        forceExit(code);
    }

    public static boolean contains(String[] array, String value) {
        for(String s : array) {
            if(s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String... args) throws Throwable {
        JVMHelper.checkStackTrace(LauncherEngineWrapper.class);
        JVMHelper.verifySystemProperties(Launcher.class, false);
        {
            LauncherEngine.checkClass(LauncherEngine.class.getClassLoader().getClass());
        }
        EnvHelper.checkDangerousParams();
        //if(!LauncherAgent.isStarted()) throw new SecurityException("JavaAgent not set");
        verifyNoAgent();
        if(contains(args, "--log-output") && Launcher.getConfig().environment != LauncherConfig.LauncherEnvironment.PROD) {
            LogHelper.addOutput(Paths.get("Launcher.log"));
        }
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        LauncherEngine.checkClass(LauncherEngineWrapper.class);
        LauncherEngine.checkClass(LauncherEngine.class);
        LauncherEngine.checkClass(ClientLauncherEntryPoint.class);
        LauncherEngine.modulesManager = new RuntimeModuleManager();
        LauncherEngine.modulesManager.loadModule(new RuntimeLauncherCoreModule());
        LauncherConfig.initModules(LauncherEngine.modulesManager);
        LauncherEngine.modulesManager.initModules(null);
        // Start Launcher
        initGson(LauncherEngine.modulesManager);
        ConsoleManager.initConsole();
        LauncherEngine.modulesManager.invokeEvent(new PreConfigPhase());
        Launcher.getConfig(); // init config
        long startTime = System.currentTimeMillis();
        try {
            newInstance(false).start(args);
        } catch (Exception e) {
            logger.error("", e);
            return;
        }
        long endTime = System.currentTimeMillis();
        logger.debug("Launcher started in {}ms", endTime - startTime);
        LauncherEngine.exitLauncher(0);
    }

    public static void initGson(RuntimeModuleManager modulesManager) {
        AuthRequest.registerProviders();
        GetAvailabilityAuthRequest.registerProviders();
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();
        Launcher.gsonManager = new RuntimeGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
    }

    public static void verifyNoAgent() {
        if (JVMHelper.RUNTIME_MXBEAN.getInputArguments().stream().filter(e -> e != null && !e.isEmpty()).anyMatch(e -> e.contains("javaagent")))
            throw new SecurityException("JavaAgent found");
    }

    public static RequestService initOffline() {
        OfflineRequestService service = new OfflineRequestService();
        ClientLauncherMethods.applyBasicOfflineProcessors(service);
        OfflineModeEvent event = new OfflineModeEvent(service);
        modulesManager.invokeEvent(event);
        return event.service;
    }

    public static LauncherEngine newInstance(boolean clientInstance) {
        return new LauncherEngine(clientInstance, NoRuntimeProvider.class);
    }

    public static LauncherEngine newInstance(boolean clientInstance, Class<? extends RuntimeProvider> basicRuntimeProvider) {
        return new LauncherEngine(clientInstance, basicRuntimeProvider);
    }

    public void start(String... args) throws Throwable {
        var config = Launcher.getConfig();
        config.apply();
        //Launcher.modulesManager = new ClientModuleManager(this);
        ClientPreGuiPhase event = new ClientPreGuiPhase(null);
        LauncherEngine.modulesManager.invokeEvent(event);
        runtimeProvider = event.runtimeProvider;
        if (runtimeProvider == null) runtimeProvider = basicRuntimeProvider.getConstructor().newInstance();
        runtimeProvider.init(clientInstance);
        //runtimeProvider.preLoad();
        if (!Request.isAvailable()) {
            String address = config.address;
            logger.debug("Start async connection to {}", address);
            RequestService service;
            try {
                service = StdWebSocketService.initWebSockets(address).get();
            } catch (Throwable e) {
                if (true) {
                    logger.error("", e);
                }
                logger.warn("Launcher in offline mode");
                service = initOffline();
            }
            Request.setRequestService(service);
            if (service instanceof StdWebSocketService) {
                ((StdWebSocketService) service).reconnectCallback = () ->
                {
                    logger.debug("WebSocket connect closed. Try reconnect");
                    try {
                        Request.reconnect();
                    } catch (Exception e) {
                        logger.error("", e);
                        throw new RequestException("Connection failed", e);
                    }
                };
            }
        }
        Request.startAutoRefresh();
        Request.getRequestService().registerEventHandler(new BasicLauncherEventHandler());
        // Init New API
        LauncherAPIHolder.setCoreAPI(new RequestCoreFeatureAPIImpl(Request.getRequestService()));
        LauncherAPIHolder.setCreateApiFactory((authId) -> {
            var impl = new RequestFeatureAPIImpl(Request.getRequestService(), authId);
            return new LauncherAPI(Map.of(
                    AuthFeatureAPI.class, impl,
                    UserFeatureAPI.class, impl,
                    ProfileFeatureAPI.class, impl,
                    TextureUploadFeatureAPI.class, impl,
                    HardwareVerificationFeatureAPI.class, impl));
        });
        LauncherBackendAPIHolder.setApi(new LauncherBackendImpl());
        //
        Objects.requireNonNull(args, "args");
        if (started.getAndSet(true))
            throw new IllegalStateException("Launcher has been already started");
        registerCommands();
        LauncherEngine.modulesManager.invokeEvent(new ClientEngineInitPhase(this));
        runtimeProvider.preLoad();
        logger.debug("Dir: {}", DirBridge.dir);
        runtimeProvider.run(args);
    }

    private void registerCommands() {
        ConsoleManager.handler.registerCommand("modules", new ModulesCommand());
    }
}