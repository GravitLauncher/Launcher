package pro.gravit.launchserver.auth.updates;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.ProviderMap;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
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

    public abstract void pushUpdate(Map<UpdateVariant, Path> files) throws IOException;
    public abstract UpdateInfo checkUpdates(UpdateVariant variant, byte[] digest);

    public void close() {
    }

    public enum UpdateVariant {
        JAR, EXE
    }

    public record UpdateInfo(String url) {

    }
}
