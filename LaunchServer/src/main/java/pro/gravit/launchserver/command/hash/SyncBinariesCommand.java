package pro.gravit.launchserver.command.hash;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.IOException;

public final class SyncBinariesCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public SyncBinariesCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Resync launcher binaries";
    }

    @Override
    public void invoke(String... args) throws IOException {
        server.syncLauncherBinaries();
        logger.info("Binaries successfully resynced");
    }
}
