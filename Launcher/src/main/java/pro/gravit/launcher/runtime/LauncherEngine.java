package pro.gravit.launcher.runtime;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.client.*;
import pro.gravit.launcher.runtime.client.*;
import pro.gravit.launcher.runtime.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.runtime.client.events.ClientPreGuiPhase;
import pro.gravit.launcher.runtime.console.GetPublicKeyCommand;
import pro.gravit.launcher.runtime.console.ModulesCommand;
import pro.gravit.launcher.runtime.console.SignDataCommand;
import pro.gravit.launcher.runtime.gui.NoRuntimeProvider;
import pro.gravit.launcher.runtime.gui.RuntimeProvider;
import pro.gravit.launcher.runtime.managers.ConsoleManager;
import pro.gravit.launcher.client.utils.NativeJVMHalt;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.base.modules.events.OfflineModeEvent;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.base.request.RequestService;
import pro.gravit.launcher.base.request.auth.*;
import pro.gravit.launcher.base.request.websockets.OfflineRequestService;
import pro.gravit.launcher.base.request.websockets.StdWebSocketService;
import pro.gravit.launcher.start.RuntimeModuleManager;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherEngine {
    public static ClientParams clientParams;
    public static RuntimeModuleManager modulesManager;
    public final boolean clientInstance;
    // Instance
    private final AtomicBoolean started = new AtomicBoolean(false);
    public RuntimeProvider runtimeProvider;
    public ECPublicKey publicKey;
    public ECPrivateKey privateKey;
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
            LogHelper.error(e);
            return;
        }
        long endTime = System.currentTimeMillis();
        LogHelper.debug("Launcher started in %dms", endTime - startTime);
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

    public ECPublicKey getClientPublicKey() {
        return publicKey;
    }

    public byte[] sign(byte[] bytes) {
        return SecurityHelper.sign(bytes, privateKey);
    }

    public void readKeys() throws IOException, InvalidKeySpecException {
        if (privateKey != null || publicKey != null) return;
        Path dir = DirBridge.dir;
        Path publicKeyFile = dir.resolve("public.key");
        Path privateKeyFile = dir.resolve("private.key");
        if (IOHelper.isFile(publicKeyFile) && IOHelper.isFile(privateKeyFile)) {
            LogHelper.info("Reading EC keypair");
            publicKey = SecurityHelper.toPublicECDSAKey(IOHelper.read(publicKeyFile));
            privateKey = SecurityHelper.toPrivateECDSAKey(IOHelper.read(privateKeyFile));
        } else {
            LogHelper.info("Generating EC keypair");
            KeyPair pair = SecurityHelper.genECDSAKeyPair(new SecureRandom());
            publicKey = (ECPublicKey) pair.getPublic();
            privateKey = (ECPrivateKey) pair.getPrivate();

            // Write key pair list
            LogHelper.info("Writing EC keypair list");
            IOHelper.write(publicKeyFile, publicKey.getEncoded());
            IOHelper.write(privateKeyFile, privateKey.getEncoded());
        }
    }

    public void start(String... args) throws Throwable {
        //Launcher.modulesManager = new ClientModuleManager(this);
        ClientPreGuiPhase event = new ClientPreGuiPhase(null);
        LauncherEngine.modulesManager.invokeEvent(event);
        runtimeProvider = event.runtimeProvider;
        if (runtimeProvider == null) runtimeProvider = basicRuntimeProvider.getConstructor().newInstance();
        runtimeProvider.init(clientInstance);
        //runtimeProvider.preLoad();
        if (!Request.isAvailable()) {
            String address = Launcher.getConfig().address;
            LogHelper.debug("Start async connection to %s", address);
            RequestService service;
            try {
                service = StdWebSocketService.initWebSockets(address).get();
            } catch (Throwable e) {
                if (LogHelper.isDebugEnabled()) {
                    LogHelper.error(e);
                }
                LogHelper.warning("Launcher in offline mode");
                service = initOffline();
            }
            Request.setRequestService(service);
            if (service instanceof StdWebSocketService) {
                ((StdWebSocketService) service).reconnectCallback = () ->
                {
                    LogHelper.debug("WebSocket connect closed. Try reconnect");
                    try {
                        Request.reconnect();
                    } catch (Exception e) {
                        LogHelper.error(e);
                        throw new RequestException("Connection failed", e);
                    }
                };
            }
        }
        Request.startAutoRefresh();
        Request.getRequestService().registerEventHandler(new BasicLauncherEventHandler());
        Objects.requireNonNull(args, "args");
        if (started.getAndSet(true))
            throw new IllegalStateException("Launcher has been already started");
        readKeys();
        registerCommands();
        LauncherEngine.modulesManager.invokeEvent(new ClientEngineInitPhase(this));
        runtimeProvider.preLoad();
        LogHelper.debug("Dir: %s", DirBridge.dir);
        runtimeProvider.run(args);
    }

    private void registerCommands() {
        ConsoleManager.handler.registerCommand("getpublickey", new GetPublicKeyCommand(this));
        ConsoleManager.handler.registerCommand("signdata", new SignDataCommand(this));
        ConsoleManager.handler.registerCommand("modules", new ModulesCommand());
    }
}
