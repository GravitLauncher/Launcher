package pro.gravit.launchserver.launchermodules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.utils.command.Command;

public class SyncLauncherModulesCommand extends Command {
    private final LauncherModuleLoader mod;
    private transient final Logger logger = LogManager.getLogger();

    public SyncLauncherModulesCommand(LauncherModuleLoader mod) {
        this.mod = mod;
    }


    @Override
    public String getArgsDescription() {
        return "Resync launcher modules";
    }

    @Override
    public String getUsageDescription() {
        return "[]";
    }

    @Override
    public void invoke(String... args) throws Exception {
        mod.syncModules();
        logger.info("Launcher Modules synced");
    }
}
