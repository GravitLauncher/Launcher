package pro.gravit.launcher.client;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;
import pro.gravit.launcher.base.profiles.optional.triggers.OptionalTrigger;
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
        OptionalAction.registerProviders();
        OptionalTrigger.registerProviders();
        Launcher.gsonManager = new ClientGsonManager(moduleManager);
        Launcher.gsonManager.initGson();
    }
}
