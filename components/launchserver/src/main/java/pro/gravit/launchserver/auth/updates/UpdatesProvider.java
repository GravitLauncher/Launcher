package pro.gravit.launchserver.auth.updates;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public abstract class UpdatesProvider {

    public static final ProviderMap<UpdatesProvider> providers = new ProviderMap<>("UpdatesProvider");
    private static boolean registredProviders = false;
    protected transient LaunchServer server;

    public static void registerProviders() {
        if (!registredProviders) {
            providers.register("local", LocalUpdatesProvider.class);
            registredProviders = true;
        }
    }

    public void init(LaunchServer server) {
        this.server = server;
    }

    public abstract void pushUpdate(List<UpdateUploadInfo> files) throws IOException;
    public abstract UpdateInfo checkUpdates(UpdateVariant variant, BuildSecretsCheck buildSecretsCheck);

    protected boolean checkSecureHash(String secureHash, String secureSalt, String privateSecureToken) {
        if (secureHash == null || secureSalt == null) return false;
        byte[] normal_hash = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256,
                privateSecureToken.concat(".").concat(secureSalt));
        byte[] launcher_hash = Base64.getDecoder().decode(secureHash);
        return Arrays.equals(normal_hash, launcher_hash);
    }

    public void close() {
    }

    public enum UpdateVariant {
        JAR, EXE
    }

    public record UpdateInfo(String url) {

    }

    public record UpdateUploadInfo(Path path, UpdateVariant variant, BuildSecrets secrets) {

    }

    public record BuildSecrets(String secureToken, byte[] digest) {

    }

    public record BuildSecretsCheck(String secureHash, String secureSalt, byte[] digest) {

    }
}
