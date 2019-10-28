package pro.gravit.launcher;

import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.verify.LauncherTrustManager;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public final class LauncherConfig {
    private static final AutogenConfig config = new AutogenConfig();
    private static final SecureAutogenConfig secureConfig = new SecureAutogenConfig();
    private static final SimpleAutogenConfig runtimeConfig = new SimpleAutogenConfig();


    public static AutogenConfig getAutogenConfig() {
        return config;
    }

    // Instance
    public String address;
    @LauncherAPI
    public final String projectName;
    public final int clientPort;
    public String secretKeyClient;
    public String oemUnlockKey;
    public final LauncherTrustManager trustManager;
    @LauncherAPI
    public final ECPublicKey publicKey;

    @LauncherAPI
    public final Map<String, byte[]> runtime;
    public final boolean isWarningMissArchJava;
    public boolean isNettyEnabled;
    public LauncherEnvironment environment;

    public final String guardType;

    public final String secureCheckHash;
    public final String secureCheckSalt;
    public final String passwordEncryptKey;

    public LauncherConfig() throws IOException, InvalidKeySpecException {
        publicKey = SecurityHelper.toPublicECKey(runtimeConfig.key);
        secureCheckHash = config.secureCheckHash;
        secureCheckSalt = config.secureCheckSalt;
        passwordEncryptKey = config.passwordEncryptKey;
        projectName = config.projectname;
        clientPort = config.clientPort;
        secretKeyClient = config.secretKeyClient;
        oemUnlockKey = config.oemUnlockKey;
        try {
            trustManager = new LauncherTrustManager(secureConfig.certificates);
        } catch (CertificateException e) {
            throw new IOException(e);
        }

        isWarningMissArchJava = config.isWarningMissArchJava;
        guardType = config.guardType;
        address = config.address;
        LauncherEnvironment env;
        if (config.env == 0) env = LauncherEnvironment.DEV;
        else if (config.env == 1) env = LauncherEnvironment.DEBUG;
        else if (config.env == 2) env = LauncherEnvironment.STD;
        else if (config.env == 3) env = LauncherEnvironment.PROD;
        else env = LauncherEnvironment.STD;
        Launcher.applyLauncherEnv(env);
        environment = env;
        runtime = Collections.unmodifiableMap(runtimeConfig.entries);
    }

    public enum LauncherEnvironment {
        DEV, DEBUG, STD, PROD
    }
}
