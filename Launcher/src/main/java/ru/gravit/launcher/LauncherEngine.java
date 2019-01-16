package ru.gravit.launcher;

import com.google.gson.GsonBuilder;
import ru.gravit.launcher.client.ClientModuleManager;
import ru.gravit.launcher.client.DirBridge;
import ru.gravit.launcher.client.FunctionalBridge;
import ru.gravit.launcher.client.LauncherSettings;
import ru.gravit.launcher.gui.JSRuntimeProvider;
import ru.gravit.launcher.gui.RuntimeProvider;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.EnvHelper;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

import java.time.Duration;
import java.time.Instant;
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
        Instant start = Instant.now();
        try {
            new LauncherEngine().start(args);
        } catch (Exception e) {
            LogHelper.error(e);
            return;
        }
        Instant end = Instant.now();
        LogHelper.debug("Launcher started in %dms", Duration.between(start, end).toMillis());
    }

    public static void initGson() {
        if (Launcher.gson != null) return;
        Launcher.gsonBuilder = new GsonBuilder();
        Launcher.gson = Launcher.gsonBuilder.create();
    }

    // Instance
    private final AtomicBoolean started = new AtomicBoolean(false);
    public RuntimeProvider runtimeProvider;

    private LauncherEngine() {

    }


    @LauncherAPI
    public void start(String... args) throws Throwable {
        LogHelper.debug("%d", LauncherSettings.settingsMagic);
        Launcher.modulesManager = new ClientModuleManager(this);
        LauncherConfig.getAutogenConfig().initModules(); //INIT
        Launcher.modulesManager.preInitModules();
        if (runtimeProvider == null) runtimeProvider = new JSRuntimeProvider();
        runtimeProvider.init(false);
        Objects.requireNonNull(args, "args");
        if (started.getAndSet(true))
            throw new IllegalStateException("Launcher has been already started");
        Launcher.modulesManager.initModules();
        // Load init.js script
        runtimeProvider.preLoad();
        FunctionalBridge.worker = new RequestWorker();
        CommonHelper.newThread("FX Task Worker", true, FunctionalBridge.worker).start();
        FunctionalBridge.getHWID = CommonHelper.newThread("GetHWID Thread", true, FunctionalBridge::getHWID);
        FunctionalBridge.getHWID.start();
        LogHelper.debug("Dir: %s", DirBridge.dir);
        runtimeProvider.run(args);
    }

    public static LauncherEngine clientInstance() {
        return new LauncherEngine();
    }
}
