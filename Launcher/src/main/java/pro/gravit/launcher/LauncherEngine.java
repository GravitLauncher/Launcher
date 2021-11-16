package pro.gravit.launcher;

import pro.gravit.launcher.client.*;
import pro.gravit.launcher.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.client.events.ClientPreGuiPhase;
import pro.gravit.launcher.console.GetPublicKeyCommand;
import pro.gravit.launcher.console.SignDataCommand;
import pro.gravit.launcher.events.request.*;
import pro.gravit.launcher.guard.LauncherGuardInterface;
import pro.gravit.launcher.guard.LauncherGuardManager;
import pro.gravit.launcher.guard.LauncherNoGuard;
import pro.gravit.launcher.guard.LauncherWrapperGuard;
import pro.gravit.launcher.gui.NoRuntimeProvider;
import pro.gravit.launcher.gui.RuntimeProvider;
import pro.gravit.launcher.managers.ClientGsonManager;
import pro.gravit.launcher.managers.ConsoleManager;
import pro.gravit.launcher.modules.events.OfflineModeEvent;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.RequestService;
import pro.gravit.launcher.request.auth.*;
import pro.gravit.launcher.request.auth.details.AuthLoginOnlyDetails;
import pro.gravit.launcher.request.management.FeaturesRequest;
import pro.gravit.launcher.request.secure.GetSecureLevelInfoRequest;
import pro.gravit.launcher.request.secure.SecurityReportRequest;
import pro.gravit.launcher.request.update.LauncherRequest;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;
import pro.gravit.launcher.request.websockets.OfflineRequestService;
import pro.gravit.launcher.request.websockets.StdWebSocketService;
import pro.gravit.launcher.utils.NativeJVMHalt;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherEngine {
    public static ClientLauncherProcess.ClientParams clientParams;
    public static LauncherGuardInterface guard;
    public static ClientModuleManager modulesManager;
    public final boolean clientInstance;
    // Instance
    private final AtomicBoolean started = new AtomicBoolean(false);
    public RuntimeProvider runtimeProvider;
    public ECPublicKey publicKey;
    public ECPrivateKey privateKey;

    private LauncherEngine(boolean clientInstance) {

        this.clientInstance = clientInstance;
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

    public static void exitLauncher(int code) {
        modulesManager.invokeEvent(new ClientExitPhase(code));
        try {
            System.exit(code);
        } catch (Throwable e) //Forge Security Manager?
        {
            NativeJVMHalt.haltA(code);
        }
    }

    public static void main(String... args) throws Throwable {
        JVMHelper.checkStackTrace(LauncherEngine.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParams();
        //if(!LauncherAgent.isStarted()) throw new SecurityException("JavaAgent not set");
        verifyNoAgent();
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        LauncherEngine.checkClass(LauncherEngine.class);
        LauncherEngine.checkClass(LauncherAgent.class);
        LauncherEngine.checkClass(ClientLauncherEntryPoint.class);
        LauncherEngine.modulesManager = new ClientModuleManager();
        LauncherEngine.modulesManager.loadModule(new ClientLauncherCoreModule());
        LauncherConfig.initModules(LauncherEngine.modulesManager);
        LauncherEngine.modulesManager.initModules(null);
        // Start Launcher
        initGson(LauncherEngine.modulesManager);
        ConsoleManager.initConsole();
        LauncherEngine.modulesManager.invokeEvent(new PreConfigPhase());
        Launcher.getConfig(); // init config
        long startTime = System.currentTimeMillis();
        try {
            new LauncherEngine(false).start(args);
        } catch (Exception e) {
            LogHelper.error(e);
            return;
        }
        long endTime = System.currentTimeMillis();
        LogHelper.debug("Launcher started in %dms", endTime - startTime);
        //Request.service.close();
        //FunctionalBridge.close();
        LauncherEngine.exitLauncher(0);
    }

    public static void initGson(ClientModuleManager modulesManager) {
        AuthRequest.registerProviders();
        GetAvailabilityAuthRequest.registerProviders();
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();
        Launcher.gsonManager = new ClientGsonManager(modulesManager);
        Launcher.gsonManager.initGson();
    }

    public static void verifyNoAgent() {
        if (JVMHelper.RUNTIME_MXBEAN.getInputArguments().stream().filter(e -> e != null && !e.isEmpty()).anyMatch(e -> e.contains("javaagent")))
            throw new SecurityException("JavaAgent found");
    }

    public static LauncherGuardInterface tryGetStdGuard() {
        switch (Launcher.getConfig().guardType) {
            case "no":
                return new LauncherNoGuard();
            case "wrapper":
                return new LauncherWrapperGuard();
        }
        return null;
    }

    public static RequestService initOffline() {
        OfflineRequestService service = new OfflineRequestService();
        applyBasicOfflineProcessors(service);
        OfflineModeEvent event = new OfflineModeEvent(service);
        modulesManager.invokeEvent(event);
        return event.service;
    }

    public static void applyBasicOfflineProcessors(OfflineRequestService service) {
        service.registerRequestProcessor(LauncherRequest.class, (r) -> new LauncherRequestEvent(false, (String) null));
        service.registerRequestProcessor(CheckServerRequest.class, (r) -> {
            throw new RequestException("CheckServer disabled in offline mode");
        });
        service.registerRequestProcessor(GetAvailabilityAuthRequest.class, (r) -> {
            List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> details = new ArrayList<>();
            details.add(new AuthLoginOnlyDetails());
            GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability = new GetAvailabilityAuthRequestEvent.AuthAvailability("offline", "Offline Mode", details);
            List<GetAvailabilityAuthRequestEvent.AuthAvailability> list = new ArrayList<>(1);
            list.add(authAvailability);
            return new GetAvailabilityAuthRequestEvent(list);
        });
        service.registerRequestProcessor(JoinServerRequest.class, (r) -> new JoinServerRequestEvent(false));
        service.registerRequestProcessor(ExitRequest.class, (r) -> new ExitRequestEvent(ExitRequestEvent.ExitReason.CLIENT));
        service.registerRequestProcessor(SetProfileRequest.class, (r) -> new SetProfileRequestEvent(null));
        service.registerRequestProcessor(FeaturesRequest.class, (r) -> new FeaturesRequestEvent());
        service.registerRequestProcessor(GetSecureLevelInfoRequest.class, (r) -> new GetSecureLevelInfoRequestEvent(null, false));
        service.registerRequestProcessor(SecurityReportRequest.class, (r) -> new SecurityReportRequestEvent(SecurityReportRequestEvent.ReportAction.NONE));
    }

    public static LauncherEngine clientInstance() {
        return new LauncherEngine(true);
    }

    public static LauncherEngine newInstance(boolean clientInstance) {
        return new LauncherEngine(clientInstance);
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
        LauncherEngine.guard = tryGetStdGuard();
        ClientPreGuiPhase event = new ClientPreGuiPhase(null);
        LauncherEngine.modulesManager.invokeEvent(event);
        runtimeProvider = event.runtimeProvider;
        if (runtimeProvider == null) runtimeProvider = new NoRuntimeProvider();
        runtimeProvider.init(clientInstance);
        //runtimeProvider.preLoad();
        if (!Request.isAvailable()) {
            String address = Launcher.getConfig().address;
            LogHelper.debug("Start async connection to %s", address);
            RequestService service;
            try {
                service = StdWebSocketService.initWebSockets(address).get();
            } catch (Throwable e) {
                if(LogHelper.isDebugEnabled()) {
                    LogHelper.error(e);
                }
                LogHelper.warning("Launcher in offline mode");
                service = initOffline();
            }
            Request.setRequestService(service);
            if(service instanceof StdWebSocketService) {
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
            service.registerEventHandler(new BasicLauncherEventHandler());
        }
        Objects.requireNonNull(args, "args");
        if (started.getAndSet(true))
            throw new IllegalStateException("Launcher has been already started");
        readKeys();
        registerCommands();
        LauncherEngine.modulesManager.invokeEvent(new ClientEngineInitPhase(this));
        runtimeProvider.preLoad();
        LauncherGuardManager.initGuard(clientInstance);
        LogHelper.debug("Dir: %s", DirBridge.dir);
        runtimeProvider.run(args);
    }

    private void registerCommands() {
        ConsoleManager.handler.registerCommand("getpublickey", new GetPublicKeyCommand(this));
        ConsoleManager.handler.registerCommand("signdata", new SignDataCommand(this));
    }
}
