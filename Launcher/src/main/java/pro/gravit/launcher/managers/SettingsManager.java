package pro.gravit.launcher.managers;

import pro.gravit.launcher.NewLauncherSettings;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class SettingsManager extends JsonConfigurable<NewLauncherSettings> {
    public static NewLauncherSettings settings;


    public SettingsManager() {
        super(NewLauncherSettings.class, DirBridge.dir.resolve("settings.json"));
    }

    @Override
    public NewLauncherSettings getConfig() {
        return settings;
    }

    @Override
    public void setConfig(NewLauncherSettings config) {
        settings = config;
        if (settings.consoleUnlockKey != null && !ConsoleManager.isConsoleUnlock) {
            if (ConsoleManager.checkUnlockKey(settings.consoleUnlockKey)) {
                ConsoleManager.unlock();
                LogHelper.info("Console auto unlocked");
            }
        }
    }

    @Override
    public NewLauncherSettings getDefaultConfig() {
        return new NewLauncherSettings();
    }
}
