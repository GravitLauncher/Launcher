package pro.gravit.launcher;

import pro.gravit.launcher.client.*;
import pro.gravit.launcher.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.ClientPreGuiPhase;
import pro.gravit.launcher.guard.LauncherGuardManager;
import pro.gravit.launcher.gui.NoRuntimeProvider;
import pro.gravit.launcher.gui.RuntimeProvider;
import pro.gravit.launcher.hwid.HWIDProvider;
import pro.gravit.launcher.managers.ClientGsonManager;
import pro.gravit.launcher.managers.ClientHookManager;
import pro.gravit.launcher.managers.ConsoleManager;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.auth.RestoreSessionRequest;
import pro.gravit.launcher.request.update.UpdateRequest;
import pro.gravit.launcher.request.websockets.StandartClientWebSocketService;
import pro.gravit.utils.helper.*;
import pro.gravit.utils.verify.LauncherTrustManager;

import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherEngine {
    public static final AtomicBoolean IS_CLIENT = new AtomicBoolean(false);

    public static void checkClass(Class<?> clazz) throws SecurityException {
        LauncherTrustManager trustManager = Launcher.getConfig().trustManager;
        if (trustManager == null) return;
        X509Certificate[] certificates = JVMHelper.getCertificates(clazz);
        if (certificates == null) {
            throw new SecurityException(String.format("Class %s not signed", clazz.getName()));
        }
        try {
            trustManager.checkCertificate(certificates, (c, s) -> {

            });
        } catch (CertificateException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new SecurityException(e);
        }
    }

    public static void main(String... args) throws Throwable {
        JVMHelper.checkStackTrace(LauncherEngine.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParams();
        //if(!LauncherAgent.isStarted()) throw new SecurityException("JavaAgent not set");
        JVMHelper.verifyNoAgent();
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        LauncherEngine.checkClass(LauncherEngine.class);
        LauncherEngine.checkClass(LauncherAgent.class);
        LauncherEngine.checkClass(ClientLauncher.class);
        LauncherEngine.modulesManager = new ClientModuleManager();
        LauncherConfig.getAutogenConfig().initModules();
        LauncherEngine.modulesManager.initModules(null);
        // Start Launcher
        initGson(LauncherEngine.modulesManager);
        ConsoleManager.initConsole();
        HWIDProvider.registerHWIDs();
        LauncherEngine.modulesManager.invokeEvent(new PreConfigPhase());
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

    public static void initGson(ClientModuleManager modulesManager) {
        Launcher.gsonManager = new ClientGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
    }

    public void readKeys() throws IOException, InvalidKeySpecException {
        if (privateKey != null || publicKey != null) return;
        Path dir = DirBridge.dir;
        Path publicKeyFile = dir.resolve("public.key");
        Path privateKeyFile = dir.resolve("private.key");
        if (IOHelper.isFile(publicKeyFile) && IOHelper.isFile(privateKeyFile)) {
            LogHelper.info("Reading EC keypair");
            publicKey = SecurityHelper.toPublicECKey(IOHelper.read(publicKeyFile));
            privateKey = SecurityHelper.toPrivateECKey(IOHelper.read(privateKeyFile));
        } else {
            LogHelper.info("Generating EC keypair");
            KeyPair pair = SecurityHelper.genECKeyPair(new SecureRandom());
            publicKey = (ECPublicKey) pair.getPublic();
            privateKey = (ECPrivateKey) pair.getPrivate();

            // Write key pair list
            LogHelper.info("Writing EC keypair list");
            IOHelper.write(publicKeyFile, publicKey.getEncoded());
            IOHelper.write(privateKeyFile, privateKey.getEncoded());
        }
    }

    // Instance
    private final AtomicBoolean started = new AtomicBoolean(false);
    public RuntimeProvider runtimeProvider;
    public ECPublicKey publicKey;
    public ECPrivateKey privateKey;

    public static ClientModuleManager modulesManager;

    private LauncherEngine() {

    }


    @LauncherAPI
    public void start(String... args) throws Throwable {
        //Launcher.modulesManager = new ClientModuleManager(this);
        ClientPreGuiPhase event = new ClientPreGuiPhase(null);
        LauncherEngine.modulesManager.invokeEvent(event);
        runtimeProvider = event.runtimeProvider;
        if (runtimeProvider == null) runtimeProvider = new NoRuntimeProvider();
        ClientHookManager.initGuiHook.hook(runtimeProvider);
        runtimeProvider.init(false);
        //runtimeProvider.preLoad();
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
        if (UpdateRequest.getController() == null) UpdateRequest.setController(new LauncherUpdateController());
        Objects.requireNonNull(args, "args");
        if (started.getAndSet(true))
            throw new IllegalStateException("Launcher has been already started");
        readKeys();
        LauncherEngine.modulesManager.invokeEvent(new ClientEngineInitPhase(this));
        runtimeProvider.preLoad();
        LauncherGuardManager.initGuard(false);
        LogHelper.debug("Dir: %s", DirBridge.dir);
        runtimeProvider.run(args);
    }

    public static LauncherEngine clientInstance() {
        return new LauncherEngine();
    }
}
