package pro.gravit.launcher.client;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.base.events.request.*;
import pro.gravit.launcher.base.modules.LauncherModulesManager;
import pro.gravit.launcher.base.modules.events.OfflineModeEvent;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.base.request.RequestService;
import pro.gravit.launcher.base.request.auth.*;
import pro.gravit.launcher.base.request.auth.details.AuthLoginOnlyDetails;
import pro.gravit.launcher.base.request.management.FeaturesRequest;
import pro.gravit.launcher.base.request.secure.GetSecureLevelInfoRequest;
import pro.gravit.launcher.base.request.secure.SecurityReportRequest;
import pro.gravit.launcher.base.request.update.LauncherRequest;
import pro.gravit.launcher.base.request.uuid.ProfileByUUIDRequest;
import pro.gravit.launcher.base.request.uuid.ProfileByUsernameRequest;
import pro.gravit.launcher.base.request.websockets.OfflineRequestService;
import pro.gravit.launcher.client.utils.NativeJVMHalt;
import pro.gravit.utils.helper.JVMHelper;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ClientLauncherMethods {

    public static void verifyNoAgent() {
        if (JVMHelper.RUNTIME_MXBEAN.getInputArguments().stream().filter(e -> e != null && !e.isEmpty()).anyMatch(e -> e.contains("javaagent")))
            throw new SecurityException("JavaAgent found");
    }

    //JVMHelper.getCertificates
    public static X509Certificate[] getCertificates(Class<?> clazz) {
        Object[] signers = clazz.getSigners();
        if (signers == null) return null;
        return Arrays.stream(signers).filter((c) -> c instanceof X509Certificate).map((c) -> (X509Certificate) c).toArray(X509Certificate[]::new);
    }



    public static void beforeExit(int code) {
        try {
            ClientLauncherEntryPoint.modulesManager.invokeEvent(new ClientExitPhase(code));
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

    public static void initGson(ClientModuleManager moduleManager) {
        AuthRequest.registerProviders();
        GetAvailabilityAuthRequest.registerProviders();
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();
        Launcher.gsonManager = new ClientGsonManager(moduleManager);
        Launcher.gsonManager.initGson();
    }

    public static RequestService initOffline(LauncherModulesManager modulesManager, ClientParams params) {
        OfflineRequestService service = new OfflineRequestService();
        applyBasicOfflineProcessors(service);
        applyClientOfflineProcessors(service, params);
        OfflineModeEvent event = new OfflineModeEvent(service);
        modulesManager.invokeEvent(event);
        return event.service;
    }

    public static void applyClientOfflineProcessors(OfflineRequestService service, ClientParams params) {
        service.registerRequestProcessor(ProfileByUsernameRequest.class, (r) -> {
            if (params.playerProfile.username.equals(r.username)) {
                return new ProfileByUsernameRequestEvent(params.playerProfile);
            }
            throw new RequestException("User not found");
        });
        service.registerRequestProcessor(ProfileByUUIDRequest.class, (r) -> {
            if (params.playerProfile.uuid.equals(r.uuid)) {
                return new ProfileByUUIDRequestEvent(params.playerProfile);
            }
            throw new RequestException("User not found");
        });
    }



    public static void applyBasicOfflineProcessors(OfflineRequestService service) {
        service.registerRequestProcessor(LauncherRequest.class, (r) -> new LauncherRequestEvent(false, (String) null));
        service.registerRequestProcessor(CheckServerRequest.class, (r) -> {
            throw new RequestException("CheckServer disabled in offline mode");
        });
        service.registerRequestProcessor(GetAvailabilityAuthRequest.class, (r) -> {
            List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> details = new ArrayList<>();
            details.add(new AuthLoginOnlyDetails());
            GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability = new GetAvailabilityAuthRequestEvent.AuthAvailability(details, "offline", "Offline Mode", true, new HashSet<>());
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
}
