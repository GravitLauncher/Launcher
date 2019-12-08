package pro.gravit.launcher;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;
import pro.gravit.utils.verify.LauncherTrustManager;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public final class LauncherConfig extends StreamObject {
    private static final AutogenConfig config = new AutogenConfig();
    private static final SecureAutogenConfig secureConfig = new SecureAutogenConfig();


    public static AutogenConfig getAutogenConfig() {
        return config;
    }

    // Instance
    public String address;

    public final String projectName;
    public final int clientPort;
    public String secretKeyClient;
    public String oemUnlockKey;
    public final LauncherTrustManager trustManager;

    public final ECPublicKey publicKey;


    public final Map<String, byte[]> runtime;
    public final boolean isWarningMissArchJava;
    public boolean isNettyEnabled;
    public LauncherEnvironment environment;

    public final String guardType;

    public final String secureCheckHash;
    public final String secureCheckSalt;
    public final String passwordEncryptKey;


    public LauncherConfig(HInput input) throws IOException, InvalidKeySpecException {
        publicKey = SecurityHelper.toPublicECKey(input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH));
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
        // Read signed runtime
        int count = input.readLength(0);
        Map<String, byte[]> localResources = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String name = input.readString(255);
            VerifyHelper.putIfAbsent(localResources, name,
                    input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH),
                    String.format("Duplicate runtime resource: '%s'", name));
        }
        runtime = Collections.unmodifiableMap(localResources);
    }


    public LauncherConfig(String address, ECPublicKey publicKey, Map<String, byte[]> runtime, String projectName) {
        this.address = address;
        this.publicKey = publicKey;
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectName = projectName;
        this.clientPort = 32148;
        guardType = "no";
        isWarningMissArchJava = true;
        isNettyEnabled = false;
        environment = LauncherEnvironment.STD;
        secureCheckSalt = null;
        secureCheckHash = null;
        passwordEncryptKey = null;
        trustManager = null;
    }

    @Override
    public void write(HOutput output) throws IOException {
        output.writeByteArray(publicKey.getEncoded(), SecurityHelper.CRYPTO_MAX_LENGTH);

        // Write signed runtime
        Set<Map.Entry<String, byte[]>> entrySet = runtime.entrySet();
        output.writeLength(entrySet.size(), 0);
        for (Map.Entry<String, byte[]> entry : runtime.entrySet()) {
            output.writeString(entry.getKey(), 255);
            output.writeByteArray(entry.getValue(), SecurityHelper.CRYPTO_MAX_LENGTH);
        }
    }

    public enum LauncherEnvironment {
        DEV, DEBUG, STD, PROD
    }
}
