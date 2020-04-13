package pro.gravit.launchserver.launchermodules;

import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class SyncLauncherModulesCommand extends Command {
    private final LauncherModuleLoader mod;

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
        LogHelper.info("Launcher Modules synced");
    }
}
