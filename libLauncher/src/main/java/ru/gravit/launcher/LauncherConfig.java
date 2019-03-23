package ru.gravit.launcher;

import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.stream.StreamObject;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public final class LauncherConfig extends StreamObject {
    private static final AutogenConfig config = new AutogenConfig();


    public static AutogenConfig getAutogenConfig() {
        return config;
    }

    // Instance
    @LauncherAPI
    public InetSocketAddress address;
    public String nettyAddress;
    public int nettyPort;
    @LauncherAPI
    public final String projectname;
    public final int clientPort;
    public String secretKeyClient;
    @LauncherAPI
    public final RSAPublicKey publicKey;

    @LauncherAPI
    public final Map<String, byte[]> runtime;

    public final boolean isUsingWrapper;
    public final boolean isDownloadJava;
    public final boolean isWarningMissArchJava;
    public final boolean isNettyEnabled;

    public final String guardLicenseName;
    public final String guardLicenseKey;
    public final String guardLicenseEncryptKey;

    @LauncherAPI
    public LauncherConfig(HInput input) throws IOException, InvalidKeySpecException {
        address = InetSocketAddress.createUnresolved( config.address, config.port);
        publicKey = SecurityHelper.toPublicRSAKey(input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH));
        projectname = config.projectname;
        clientPort = config.clientPort;
        secretKeyClient = config.secretKeyClient;
        isDownloadJava = config.isDownloadJava;
        isUsingWrapper = config.isUsingWrapper;
        isWarningMissArchJava = config.isWarningMissArchJava;
        guardLicenseEncryptKey = config.guardLicenseEncryptKey;
        guardLicenseKey = config.guardLicenseKey;
        guardLicenseName = config.guardLicenseName;
        nettyPort = config.nettyPort;
        nettyAddress = config.nettyAddress;
        isNettyEnabled = config.isNettyEnabled;
        LauncherEnvironment env;
        if (config.env == 0) env = LauncherEnvironment.DEV;
        else if (config.env == 1) env = LauncherEnvironment.DEBUG;
        else if (config.env == 2) env = LauncherEnvironment.STD;
        else if (config.env == 3) env = LauncherEnvironment.PROD;
        else env = LauncherEnvironment.STD;
        Launcher.applyLauncherEnv(env);
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
    public LauncherConfig(String address, int port, RSAPublicKey publicKey, Map<String, byte[]> runtime, String projectname) {
        this.address = InetSocketAddress.createUnresolved(address, port);
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectname = projectname;
        this.clientPort = 32148;
        this.guardLicenseName = "FREE";
        this.guardLicenseKey = "AAAA-BBBB-CCCC-DDDD";
        this.guardLicenseEncryptKey = "12345";
        isUsingWrapper = true;
        isDownloadJava = false;
        isWarningMissArchJava = true;
        isNettyEnabled = false;
    }

    @LauncherAPI
    public LauncherConfig(String address, int port, RSAPublicKey publicKey, Map<String, byte[]> runtime) {
        this.address = InetSocketAddress.createUnresolved(address, port);
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectname = "Minecraft";
        this.guardLicenseName = "FREE";
        this.guardLicenseKey = "AAAA-BBBB-CCCC-DDDD";
        this.guardLicenseEncryptKey = "12345";
        this.clientPort = 32148;
        isUsingWrapper = true;
        isDownloadJava = false;
        isWarningMissArchJava = true;
        isNettyEnabled = false;
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
