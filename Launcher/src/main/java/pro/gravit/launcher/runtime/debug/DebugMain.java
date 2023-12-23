package pro.gravit.launcher.runtime.debug;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.client.ClientLauncherMethods;
import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.client.RuntimeLauncherCoreModule;
import pro.gravit.launcher.runtime.managers.ConsoleManager;
import pro.gravit.launcher.start.RuntimeModuleManager;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.events.OfflineModeEvent;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.RequestService;
import pro.gravit.launcher.base.request.websockets.OfflineRequestService;
import pro.gravit.launcher.base.request.websockets.StdWebSocketService;
import pro.gravit.utils.helper.LogHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebugMain {
    public static final AtomicBoolean IS_DEBUG = new AtomicBoolean(false);
    public static String webSocketURL = System.getProperty("launcherdebug.websocket", "ws://localhost:9274/api");
    public static String projectName = System.getProperty("launcherdebug.projectname", "Minecraft");
    public static String unlockSecret = System.getProperty("launcherdebug.unlocksecret", "");
    public static boolean offlineMode = Boolean.getBoolean("launcherdebug.offlinemode");
    public static boolean disableAutoRefresh = Boolean.getBoolean("launcherdebug.disableautorefresh");
    public static String[] moduleClasses = System.getProperty("launcherdebug.modules", "").split(",");
    public static String[] moduleFiles = System.getProperty("launcherdebug.modulefiles", "").split(",");
    public static LauncherConfig.LauncherEnvironment environment = LauncherConfig.LauncherEnvironment.valueOf(System.getProperty("launcherdebug.env", "STD"));

    public static void main(String[] args) throws Throwable {
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        IS_DEBUG.set(true);
        LogHelper.info("Launcher start in DEBUG mode (Only for developers)");
        LogHelper.debug("Initialization LauncherConfig");
        LauncherConfig config = new LauncherConfig(webSocketURL, new HashMap<>(), projectName, environment, new DebugLauncherTrustManager(DebugLauncherTrustManager.TrustDebugMode.TRUST_ALL));
        config.unlockSecret = unlockSecret;
        Launcher.setConfig(config);
        Launcher.applyLauncherEnv(environment);
        LauncherEngine.modulesManager = new RuntimeModuleManager();
        LauncherEngine.modulesManager.loadModule(new RuntimeLauncherCoreModule());
        for (String moduleClassName : moduleClasses) {
            if (moduleClassName.isEmpty()) continue;
            LauncherEngine.modulesManager.loadModule(newModule(moduleClassName));
        }
        for (String moduleFileName : moduleFiles) {
            if (moduleFileName.isEmpty()) continue;
            LauncherEngine.modulesManager.loadModule(Paths.get(moduleFileName));
        }
        LauncherEngine.modulesManager.initModules(null);
        LauncherEngine.initGson(LauncherEngine.modulesManager);
        ConsoleManager.initConsole();
        LauncherEngine.modulesManager.invokeEvent(new PreConfigPhase());
        RequestService service;
        if (offlineMode) {
            OfflineRequestService offlineRequestService = new OfflineRequestService();
            ClientLauncherMethods.applyBasicOfflineProcessors(offlineRequestService);
            OfflineModeEvent event = new OfflineModeEvent(offlineRequestService);
            LauncherEngine.modulesManager.invokeEvent(event);
            service = event.service;
        } else {
            service = StdWebSocketService.initWebSockets(webSocketURL).get();
        }
        Request.setRequestService(service);
        if(!disableAutoRefresh) {
            Request.startAutoRefresh();
        }
        LogHelper.debug("Initialization LauncherEngine");
        LauncherEngine instance = LauncherEngine.newInstance(false, ClientRuntimeProvider.class);
        instance.start(args);
        LauncherEngine.exitLauncher(0);
    }

    @SuppressWarnings("unchecked")
    public static LauncherModule newModule(String className) throws ClassNotFoundException, InvocationTargetException {
        Class<? extends LauncherModule> clazz = (Class<? extends LauncherModule>) Class.forName(className);
        try {
            return (LauncherModule) MethodHandles.publicLookup().findConstructor(clazz, MethodType.methodType(void.class)).invoke();
        } catch (Throwable throwable) {
            throw new InvocationTargetException(throwable);
        }
    }
}
