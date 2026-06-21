package pro.gravit.launcher.runtime.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.client.LauncherAPIInitializer;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.method.password.AuthPlainPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.UserPermissions;
import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.client.RuntimeLauncherCoreModule;
import pro.gravit.launcher.runtime.managers.ConsoleManager;
import pro.gravit.launcher.start.RuntimeModuleManager;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.utils.helper.LogHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.*;
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
        LauncherConfig config = new LauncherConfig(DebugProperties.ADDRESS, new HashMap<>(), DebugProperties.PROJECT_NAME, DebugProperties.ENV, new DebugLauncherTrustManager(DebugLauncherTrustManager.TrustDebugMode.TRUST_ALL));
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
        List<LauncherAPIInitializer.Flag> flags = new ArrayList<>();
        if(!DebugProperties.DISABLE_AUTO_REFRESH) {
            flags.add(LauncherAPIInitializer.Flag.ENABLE_AUTO_REFRESH);
        }
        if(DebugProperties.OFFLINE_MODE) {
            flags.add(LauncherAPIInitializer.Flag.OFFLINE_MODE);
        }
        LauncherAPIInitializer.initialize(LauncherEngine.modulesManager, DebugProperties.ADDRESS, flags);
        LauncherAPIHolder.changeAuthId(DebugProperties.AUTH_ID);
    }

    public static SelfUser authorize() throws Exception {
        if(DebugProperties.ACCESS_TOKEN != null) {
            return LauncherAPIHolder.auth().restore(DebugProperties.ACCESS_TOKEN, true).get();
        } else if(DebugProperties.LOGIN != null) {
            return LauncherAPIHolder.auth().auth(DebugProperties.LOGIN, new AuthPlainPassword(DebugProperties.PASSWORD)).thenApply(AuthFeatureAPI.AuthResponse::user).get();
        } else {
            return new VirtualSelfUser();
        }
    }

    public record VirtualSelfUser() implements SelfUser {
        private static UUID PLAYER_UUID = UUID.fromString("Player");

        @Override
        public String getAccessToken() {
            return "";
        }

        @Override
        public UserPermissions getPermissions() {
            return new ClientPermissions();
        }

        @Override
        public String getUsername() {
            return "Player";
        }

        @Override
        public UUID getUUID() {
            return PLAYER_UUID;
        }

        @Override
        public Map<String, Texture> getAssets() {
            return Map.of();
        }

        @Override
        public Map<String, String> getProperties() {
            return Map.of();
        }
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
