package ru.gravit.launcher;

import ru.gravit.launcher.client.ClientModuleManager;
import ru.gravit.launcher.client.DirBridge;
import ru.gravit.launcher.client.FunctionalBridge;
import ru.gravit.launcher.guard.LauncherGuardManager;
import ru.gravit.launcher.gui.JSRuntimeProvider;
import ru.gravit.launcher.gui.RuntimeProvider;
import ru.gravit.launcher.managers.ClientGsonManager;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.StandartClientWebSocketService;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.EnvHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherEngine {

    public static void main(String... args) throws Throwable {
        JVMHelper.checkStackTrace(LauncherEngine.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParams();
        //if(!LauncherAgent.isStarted()) throw new SecurityException("JavaAgent not set");
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        // Start Launcher
        initGson();
        LogHelper.setStacktraceEnabled(true);
        long startTime = System.currentTimeMillis();
        try {
            new LauncherEngine().start(args);
        } catch (Exception e) {
            LogHelper.error(e);
            return;
        }
        long endTime = System.currentTimeMillis();
        LogHelper.debug("Launcher started in %dms", endTime - startTime);
    }

    public static void initGson() {
        Launcher.gsonManager = new ClientGsonManager();
        Launcher.gsonManager.initGson();
    }

    // Instance
    private final AtomicBoolean started = new AtomicBoolean(false);
    public RuntimeProvider runtimeProvider;

    private LauncherEngine() {

    }


    @LauncherAPI
    public void start(String... args) throws Throwable {
        Launcher.modulesManager = new ClientModuleManager(this);
        LauncherConfig.getAutogenConfig().initModules();
        Launcher.modulesManager.preInitModules();
        if (runtimeProvider == null) runtimeProvider = new JSRuntimeProvider();
        runtimeProvider.init(false);
        runtimeProvider.preLoad();
        if(Request.service != null)
        {
            String address = Launcher.getConfig().address;
            LogHelper.debug("Start async connection to %s", address);
            Request.service = StandartClientWebSocketService.initWebSockets(address, true);
        }
        LauncherGuardManager.initGuard(false);
        Objects.requireNonNull(args, "args");
        if (started.getAndSet(true))
            throw new IllegalStateException("Launcher has been already started");
        Launcher.modulesManager.initModules();
        runtimeProvider.preLoad();
        FunctionalBridge.getHWID = CommonHelper.newThread("GetHWID Thread", true, FunctionalBridge::getHWID);
        FunctionalBridge.getHWID.start();
        LogHelper.debug("Dir: %s", DirBridge.dir);
        runtimeProvider.run(args);
    }

    public static LauncherEngine clientInstance() {
        return new LauncherEngine();
    }
}
