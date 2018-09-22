package ru.gravit.launcher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.stream.StreamObject;

public final class LauncherConfig extends StreamObject {
    @LauncherAPI
    public static final String ADDRESS_OVERRIDE_PROPERTY = "launcher.addressOverride";
    @LauncherAPI
    public static final String ADDRESS_OVERRIDE = System.getProperty(ADDRESS_OVERRIDE_PROPERTY, null);
    private static final AutogenConfig config = new AutogenConfig();

    @LauncherAPI
    public static AutogenConfig getAutogenConfig() {
        return config;
    }

    // Instance
    @LauncherAPI
    public final InetSocketAddress address;
    @LauncherAPI
    public final String projectname;
    @LauncherAPI
    public final RSAPublicKey publicKey;

    @LauncherAPI
    public final Map<String, byte[]> runtime;

    @LauncherAPI
    public LauncherConfig(HInput input) throws IOException, InvalidKeySpecException {
        String localAddress = config.address;
        address = InetSocketAddress.createUnresolved(
                ADDRESS_OVERRIDE == null ? localAddress : ADDRESS_OVERRIDE, config.port);
        publicKey = SecurityHelper.toPublicRSAKey(input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH));
        projectname = config.projectname;
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

        // Print warning if address override is enabled
        if (ADDRESS_OVERRIDE != null)
            LogHelper.warning("Address override is enabled: '%s'", ADDRESS_OVERRIDE);
    }

    @LauncherAPI
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public LauncherConfig(String address, int port, RSAPublicKey publicKey, Map<String, byte[]> runtime, String projectname) {
        this.address = InetSocketAddress.createUnresolved(address, port);
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectname = projectname;
    }

    @LauncherAPI
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    public LauncherConfig(String address, int port, RSAPublicKey publicKey, Map<String, byte[]> runtime) {
        this.address = InetSocketAddress.createUnresolved(address, port);
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
        this.runtime = Collections.unmodifiableMap(new HashMap<>(runtime));
        this.projectname = "Minecraft";
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
}
