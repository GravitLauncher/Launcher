package pro.gravit.launchserver.command.hash;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SyncUpdatesCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public SyncUpdatesCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[subdirs...]";
    }

    @Override
    public String getUsageDescription() {
        return "Resync updates dir";
    }

    @Override
    public void invoke(String... args) throws IOException {
        Set<String> dirs = null;
        if (args.length > 0) { // Hash all updates dirs
            dirs = new HashSet<>(args.length);
            Collections.addAll(dirs, args);
        }

        // Hash updates dir
        server.syncUpdatesDir(dirs);
        logger.info("Updates dir successfully resynced");
    }
}
