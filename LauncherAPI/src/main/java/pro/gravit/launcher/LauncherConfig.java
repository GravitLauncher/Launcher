package pro.gravit.launcher;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModulesManager;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.stream.StreamObject;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public final class LauncherConfig extends StreamObject {
    @LauncherInject("launchercore.certificates")
    private static final List<byte[]> secureConfigCertificates = null;
    @LauncherInject("launcher.modules")
    private static final List<Class<?>> modulesClasses = null;
    private static final MethodType VOID_TYPE = MethodType.methodType(void.class);
    @LauncherInject("launcher.projectName")
    public final String projectName;
    @LauncherInject("launcher.port")
    public final int clientPort;
    public final LauncherTrustManager trustManager;
    public final ECPublicKey ecdsaPublicKey;
    public final RSAPublicKey rsaPublicKey;
    public final Map<String, byte[]> runtime;
    @LauncherInject("launcher.guardType")
    public final String guardType;
    @LauncherInject("runtimeconfig.secureCheckHash")
    public final String secureCheckHash;
    @LauncherInject("runtimeconfig.secureCheckSalt")
    public final String secureCheckSalt;
    @LauncherInject("runtimeconfig.passwordEncryptKey")
    public final String passwordEncryptKey;
    @LauncherInject("runtimeconfig.runtimeEncryptKey")
    public final String runtimeEncryptKey;
    @LauncherInject("launcher.address")
    public final String address;
    @LauncherInject("runtimeconfig.secretKeyClient")
    public String secretKeyClient;
    @LauncherInject("runtimeconfig.oemUnlockKey")
    public String oemUnlockKey;
    @LauncherInject("launchercore.env")
    public LauncherEnvironment environment;


    @LauncherInjectionConstructor
    public LauncherConfig(HInput input) throws IOException, InvalidKeySpecException {
        ecdsaPublicKey = SecurityHelper.toPublicECDSAKey(input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH));
        rsaPublicKey = SecurityHelper.toPublicRSAKey(input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH));
        secureCheckHash = null;
        secureCheckSalt = null;
        passwordEncryptKey = null;
        runtimeEncryptKey = null;
        projectName = null;
        clientPort = -1;
        secretKeyClient = null;
        oemUnlockKey = null;
        try {
            trustManager = new LauncherTrustManager(secureConfigCertificates);
        } catch (CertificateException e) {
            throw new IOException(e);
        }
        guardType = null;
        address = null;
        environment = LauncherEnvironment.STD;
        Launcher.applyLauncherEnv(environment);
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

    public LauncherConfig(String address, ECPublicKey ecdsaPublicKey, RSAPublicKey rsaPublicKey, Map<String, byte[]> runtime, String projectName) {
        this.address = address;
        this.ecdsaPublicKey = ecdsaPublicKey;
        this.rsaPublicKey = rsaPublicKey;
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectName = projectName;
        this.clientPort = 32148;
        guardType = "no";
        environment = LauncherEnvironment.STD;
        secureCheckSalt = null;
        secureCheckHash = null;
        passwordEncryptKey = null;
        runtimeEncryptKey = null;
        trustManager = null;
    }

    public LauncherConfig(String address, Map<String, byte[]> runtime, String projectName, LauncherEnvironment env, LauncherTrustManager trustManager) {
        this.address = address;
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectName = projectName;
        this.clientPort = 32148;
        this.trustManager = trustManager;
        this.rsaPublicKey = null;
        this.ecdsaPublicKey = null;
        environment = env;
        guardType = "no";
        secureCheckSalt = null;
        secureCheckHash = null;
        passwordEncryptKey = null;
        runtimeEncryptKey = null;
    }

    public static void initModules(LauncherModulesManager modulesManager) {
        for (Class<?> clazz : modulesClasses)
            try {
                modulesManager.loadModule((LauncherModule) MethodHandles.publicLookup().findConstructor(clazz, VOID_TYPE).invokeWithArguments(Collections.emptyList()));
            } catch (Throwable e) {
                LogHelper.error(e);
            }
        // This method should be called once at exec time.
        modulesClasses.clear();
    }

    @Override
    public void write(HOutput output) throws IOException {
        output.writeByteArray(ecdsaPublicKey.getEncoded(), SecurityHelper.CRYPTO_MAX_LENGTH);
        output.writeByteArray(rsaPublicKey.getEncoded(), SecurityHelper.CRYPTO_MAX_LENGTH);

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
