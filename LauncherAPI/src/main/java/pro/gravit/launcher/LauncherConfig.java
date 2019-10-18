package pro.gravit.launcher;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;
import pro.gravit.utils.verify.LauncherTrustManager;

public final class LauncherConfig extends StreamObject {
    private static final AutogenConfig config = new AutogenConfig();
    private static final SecureAutogenConfig secureConfig = new SecureAutogenConfig();


    public static AutogenConfig getAutogenConfig() {
        return config;
    }

    // Instance
    public String address;
    @LauncherAPI
    public final String projectname;
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

    public final String guardLicenseName;
    public final String guardLicenseKey;
    public final String guardLicenseEncryptKey;
    public final String guardType;
    
    public final String secureCheckHash;
    public final String secureCheckSalt;
    public final String passwordEncryptKey;

    @LauncherAPI
    public LauncherConfig(HInput input) throws IOException, InvalidKeySpecException {
        publicKey = SecurityHelper.toPublicECKey(input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH));
        secureCheckHash = config.secureCheckHash;
        secureCheckSalt = config.secureCheckSalt;
        passwordEncryptKey = config.passwordEncryptKey;
        projectname = config.projectname;
        clientPort = config.clientPort;
        secretKeyClient = config.secretKeyClient;
        oemUnlockKey = config.oemUnlockKey;
        try {
            trustManager = new LauncherTrustManager(secureConfig.certificates);
        } catch (CertificateException e) {
            throw new IOException(e);
        }

        isWarningMissArchJava = config.isWarningMissArchJava;
        guardLicenseEncryptKey = config.guardLicenseEncryptKey;
        guardLicenseKey = config.guardLicenseKey;
        guardType = config.guardType;
        guardLicenseName = config.guardLicenseName;
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

    @LauncherAPI
    public LauncherConfig(String address, ECPublicKey publicKey, Map<String, byte[]> runtime, String projectname) {
        this.address = address;
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectname = projectname;
        this.clientPort = 32148;
        this.guardLicenseName = "FREE";
        this.guardLicenseKey = "AAAA-BBBB-CCCC-DDDD";
        this.guardLicenseEncryptKey = "12345";
        guardType = "no";
        isWarningMissArchJava = true;
        isNettyEnabled = false;
        environment = LauncherEnvironment.STD;
        secureCheckSalt = null;
        secureCheckHash = null;
        passwordEncryptKey = null;
        trustManager = null;
    }

    @LauncherAPI
    public LauncherConfig(String address, ECPublicKey publicKey, Map<String, byte[]> runtime) {
        this.address = address;
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectname = "Minecraft";
        this.guardLicenseName = "FREE";
        this.guardLicenseKey = "AAAA-BBBB-CCCC-DDDD";
        this.guardLicenseEncryptKey = "12345";
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
