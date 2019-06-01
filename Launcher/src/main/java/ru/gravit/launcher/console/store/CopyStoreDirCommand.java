package ru.gravit.launcher.console.store;

import ru.gravit.launcher.NewLauncherSettings;
import ru.gravit.launcher.managers.SettingsManager;
import ru.gravit.utils.command.Command;
import ru.gravit.utils.helper.LogHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CopyStoreDirCommand extends Command {
    @Override
    public String getArgsDescription() {
        return "[index] [overwrite(true/false)]";
    }

    @Override
    public String getUsageDescription() {
        return "Copy dir in GravitLauncherStore";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        int ind = 1;
        int index = Integer.valueOf(args[0]);
        boolean overwrite = Boolean.valueOf(args[1]);
        for (NewLauncherSettings.HashedStoreEntry e : SettingsManager.settings.lastHDirs) {
            if (ind == index) {
                LogHelper.info("Copy [%d] FullPath: %s name: %s", ind, e.fullPath, e.name);
                Path path = Paths.get(e.fullPath);
                if (!Files.isDirectory(path)) {
                    LogHelper.error("Directory %s not found", path.toAbsolutePath().toString());
                    return;
                }
                Path target = Paths.get(SettingsManager.settings.updatesDirPath).resolve(e.name);
                if (Files.exists(target) && !overwrite) {
                    LogHelper.error("Directory %s found, flag overwrite not found", target.toAbsolutePath().toString());
                    return;
                }
                Files.copy(path, target);
            }
            ind++;
        }
    }
}
