package pro.gravit.launchserver.command.sync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class SyncLauncherModulesCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public SyncLauncherModulesCommand(LaunchServer server) {
        super(server);
    }


    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Resync launcher modules";
    }

    @Override
    public void invoke(String... args) throws Exception {
        server.launcherModuleLoader.syncModules();
        logger.info("Launcher Modules synced");
    }
}
