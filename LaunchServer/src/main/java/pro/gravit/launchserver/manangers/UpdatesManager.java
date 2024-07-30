package pro.gravit.launchserver.manangers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.serialize.HInput;
import pro.gravit.launcher.core.serialize.HOutput;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerUpdatesSyncEvent;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class UpdatesManager {
    private final LaunchServer server;

    public UpdatesManager(LaunchServer server) {
        this.server = server;
    }

    @Deprecated
    public void readUpdatesFromCache() throws IOException {

    }

    @Deprecated
    public void readUpdatesDir() throws IOException {

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
