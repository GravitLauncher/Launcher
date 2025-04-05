package pro.gravit.launchserver.manangers;

import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launchserver.LaunchServer;

import java.io.IOException;
import java.util.*;

public class UpdatesManager {
    private final LaunchServer server;

    public UpdatesManager(LaunchServer server) {
        this.server = server;
    }

    @Deprecated
    public void readUpdatesFromCache() {

    }

    @Deprecated
    public void readUpdatesDir() {

    }

    @Deprecated
    public void syncUpdatesDir(Collection<String> dirs) throws IOException {
        server.config.updatesProvider.sync(dirs);
    }

    @Deprecated
    public HashSet<String> getUpdatesList() {
        return new HashSet<>();
    }

    @Deprecated
    public HashedDir getUpdate(String name) {
        return server.config.updatesProvider.getUpdatesDir(name);
    }

    @Deprecated
    public void addUpdate(String name, HashedDir dir) {
        throw new UnsupportedOperationException();
    }
}
