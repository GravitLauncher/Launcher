package pro.gravit.launcher.runtime.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.CurrentUserRequestEvent;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launcher.base.request.auth.AuthRequest;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebugMain {

    private static final Logger logger =
            LoggerFactory.getLogger(DebugMain.class);

    public static final AtomicBoolean IS_DEBUG = new AtomicBoolean(false);

    public static void main(String[] args) throws Throwable {
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        initialize();
        logger.debug("Initialization LauncherEngine");
        LauncherEngine instance = LauncherEngine.newInstance(false, ClientRuntimeProvider.class);
        instance.start(args);
        LauncherEngine.exitLauncher(0);
    }

    public static void initialize() throws Exception {
        IS_DEBUG.set(true);
        logger.info("Launcher start in DEBUG mode (Only for developers)");
        logger.debug("Initialization LauncherConfig");
        LauncherConfig config = new LauncherConfig(DebugProperties.WEBSOCKET_URL, new HashMap<>(), DebugProperties.PROJECT_NAME, DebugProperties.ENV, new DebugLauncherTrustManager(DebugLauncherTrustManager.TrustDebugMode.TRUST_ALL));
        config.unlockSecret = DebugProperties.UNLOCK_SECRET;
        Launcher.setConfig(config);
        Launcher.applyLauncherEnv(DebugProperties.ENV);
        config.apply();
        LauncherEngine.modulesManager = new RuntimeModuleManager();
        LauncherEngine.modulesManager.loadModule(new RuntimeLauncherCoreModule());
        for (String moduleClassName : DebugProperties.MODULE_CLASSES) {
            if (moduleClassName.isEmpty()) continue;
            LauncherEngine.modulesManager.loadModule(newModule(moduleClassName));
        }
        for (String moduleFileName : DebugProperties.MODULE_FILES) {
            if (moduleFileName.isEmpty()) continue;
            LauncherEngine.modulesManager.loadModule(Paths.get(moduleFileName));
        }
        LauncherEngine.modulesManager.initModules(null);
        LauncherEngine.initGson(LauncherEngine.modulesManager);
        if(!DebugProperties.DISABLE_CONSOLE) {
            ConsoleManager.initConsole();
        }
        LauncherEngine.modulesManager.invokeEvent(new PreConfigPhase());
        RequestService service;
        if (DebugProperties.OFFLINE_MODE) {
            OfflineRequestService offlineRequestService = new OfflineRequestService();
            ClientLauncherMethods.applyBasicOfflineProcessors(offlineRequestService);
            OfflineModeEvent event = new OfflineModeEvent(offlineRequestService);
            LauncherEngine.modulesManager.invokeEvent(event);
            service = event.service;
        } else {
            service = StdWebSocketService.initWebSockets(DebugProperties.WEBSOCKET_URL).get();
        }
        Request.setRequestService(service);
        if(!DebugProperties.DISABLE_AUTO_REFRESH) {
            Request.startAutoRefresh();
        }
    }

    public static AuthorizationData authorize() throws Exception {
        if(DebugProperties.ACCESS_TOKEN != null) {
            Request.setOAuth(DebugProperties.AUTH_ID, new AuthRequestEvent.OAuthRequestEvent(DebugProperties.ACCESS_TOKEN, DebugProperties.REFRESH_TOKEN, DebugProperties.EXPIRE));
            Request.RequestRestoreReport report = Request.restore(true, false, true);
            return new AuthorizationData(new AuthRequestEvent.OAuthRequestEvent(DebugProperties.ACCESS_TOKEN, DebugProperties.REFRESH_TOKEN, DebugProperties.EXPIRE), report.userInfo);
        } else if(DebugProperties.LOGIN != null) {
            AuthRequest request = new AuthRequest(DebugProperties.LOGIN, DebugProperties.PASSWORD, DebugProperties.AUTH_ID, AuthRequest.ConnectTypes.API);
            AuthRequestEvent event = request.request();
            Request.setOAuth(DebugProperties.AUTH_ID, event.oauth);
            return new AuthorizationData(event.oauth, new CurrentUserRequestEvent.UserInfo(event.permissions, event.accessToken, event.playerProfile));
        } else {
            return new AuthorizationData(new AuthRequestEvent.OAuthRequestEvent("ACCESS_TOKEN", "REFRESH_TOKEN", 0),
                    new CurrentUserRequestEvent.UserInfo(new ClientPermissions(), "ACCESS_TOKEN", new PlayerProfile(UUID.fromString(DebugProperties.UUID),
                            DebugProperties.USERNAME, new HashMap<>(), new HashMap<>())));
        }
    }

    public record AuthorizationData(AuthRequestEvent.OAuthRequestEvent event, CurrentUserRequestEvent.UserInfo userInfo) {

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