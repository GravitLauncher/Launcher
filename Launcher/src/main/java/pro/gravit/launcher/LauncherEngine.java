package pro.gravit.launcher;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import pro.gravit.launcher.client.ClientModuleManager;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.FunctionalBridge;
import pro.gravit.launcher.client.LauncherUpdateController;
import pro.gravit.launcher.guard.LauncherGuardManager;
import pro.gravit.launcher.gui.JSRuntimeProvider;
import pro.gravit.launcher.gui.RuntimeProvider;
import pro.gravit.launcher.gui.GuiRuntimeProvider;
import pro.gravit.launcher.managers.ClientGsonManager;
import pro.gravit.launcher.managers.ConsoleManager;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.auth.RestoreSessionRequest;
import pro.gravit.launcher.request.update.UpdateRequest;
import pro.gravit.launcher.request.websockets.StandartClientWebSocketService;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.EnvHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

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
        ConsoleManager.initConsole();
        LauncherConfig config = Launcher.getConfig();
        if (config.environment.equals(LauncherConfig.LauncherEnvironment.PROD)) {
            if (!LauncherAgent.isStarted()) throw new SecurityException("LauncherAgent must started");
        }
        long startTime = System.currentTimeMillis();
        try {
            new LauncherEngine().start(args);
        } catch (Exception e) {
            LogHelper.error(e);
            return;
        }
        long endTime = System.currentTimeMillis();
        LogHelper.debug("Launcher started in %dms", endTime - startTime);
        //Request.service.close();
        //FunctionalBridge.close();
        System.exit(0);
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
		// INIT GUI
		try 
		{
			Class.forName( "com.jfoenixparts.controls.JFXPButton" );
			if (runtimeProvider == null) runtimeProvider = new JSRuntimeProvider();
		} 
		catch( ClassNotFoundException e ) {
			if (runtimeProvider == null) runtimeProvider = new GuiRuntimeProvider();
		}
        runtimeProvider.init(false);
        runtimeProvider.preLoad();
		// INIT GUI
        if (Request.service == null) {
            String address = Launcher.getConfig().address;
            LogHelper.debug("Start async connection to %s", address);
            Request.service = StandartClientWebSocketService.initWebSockets(address, true);
            Request.service.reconnectCallback = () ->
            {
                LogHelper.debug("WebSocket connect closed. Try reconnect");
                try {
                    Request.service.open();
                    LogHelper.debug("Connect to %s", Launcher.getConfig().address);
                } catch (Exception e) {
                    LogHelper.error(e);
                    throw new RequestException(String.format("Connect error: %s", e.getMessage() != null ? e.getMessage() : "null"));
                }
                try {
                    RestoreSessionRequest request1 = new RestoreSessionRequest(Request.getSession());
                    request1.request();
                } catch (Exception e) {
                    LogHelper.error(e);
                }
            };
        }
        LauncherGuardManager.initGuard(false);
        UpdateRequest.setController(new LauncherUpdateController());
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
