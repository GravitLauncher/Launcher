package ru.gravit.launcher.console.store;

import ru.gravit.launcher.NewLauncherSettings;
import ru.gravit.launcher.managers.SettingsManager;
import ru.gravit.utils.command.Command;
import ru.gravit.utils.helper.LogHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LinkStoreDirCommand extends Command {

    @Override
    public String getArgsDescription() {
        return "[index]";
    }

    @Override
    public String getUsageDescription() {
        return "Create symlink to GravitLauncherStore directory";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        int ind = 1;
        int index = Integer.valueOf(args[0]);
        for(NewLauncherSettings.HashedStoreEntry e : SettingsManager.settings.lastHDirs)
        {
            if(ind == index)
            {
                LogHelper.info("Copy [%d] FullPath: %s name: %s", ind, e.fullPath, e.name);
                Path path = Paths.get(e.fullPath);
                if(!Files.isDirectory(path))
                {
                    LogHelper.error("Directory %s not found", path.toAbsolutePath().toString());
                    return;
                }
                Path target = Paths.get(SettingsManager.settings.updatesDirPath).resolve(e.name);
                if(Files.exists(target))
                {
                    LogHelper.error("Directory %s already exists", target.toAbsolutePath().toString());
                    return;
                }
                Files.createSymbolicLink(path, target);
            }
            ind++;
        }
    }
}
