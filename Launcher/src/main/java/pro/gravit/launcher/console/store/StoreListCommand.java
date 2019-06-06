package pro.gravit.launcher.console.store;

import pro.gravit.launcher.NewLauncherSettings;
import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class StoreListCommand extends Command {
    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "List GravitLauncherStore";
    }

    @Override
    public void invoke(String... args) throws Exception {
        int ind = 1;
        for (NewLauncherSettings.HashedStoreEntry e : SettingsManager.settings.lastHDirs) {
            LogHelper.info("[%d] FullPath: %s name: %s", ind, e.fullPath, e.name);
            ind++;
        }
    }
}
