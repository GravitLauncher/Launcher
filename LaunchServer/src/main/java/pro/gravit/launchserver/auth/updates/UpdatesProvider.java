package pro.gravit.launchserver.auth.updates;

import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.ProviderMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
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

    public void sync() throws IOException {
        sync(null);
    }

    public abstract void syncInitially() throws IOException;

    public abstract void sync(Collection<String> updateNames) throws IOException;

    public abstract HashedDir getUpdatesDir(String updateName);

    public abstract void upload(String updateName, Map<String, Path> files, boolean deleteAfterUpload) throws IOException;

    public abstract Map<String, Path> download(String updateName, List<String> files);

    public abstract void delete(String updateName, List<String> files) throws IOException;

    public abstract void delete(String updateName) throws IOException;

    public abstract void create(String updateName) throws IOException;

    public void close() {

    }
}
