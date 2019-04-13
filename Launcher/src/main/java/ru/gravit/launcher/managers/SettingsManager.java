package ru.gravit.launcher.managers;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.NewLauncherSettings;
import ru.gravit.launcher.client.DirBridge;
import ru.gravit.utils.config.JsonConfigurable;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsManager extends JsonConfigurable<NewLauncherSettings> {
    @LauncherAPI
    public static NewLauncherSettings settings;

    public SettingsManager() {
        super(NewLauncherSettings.class, DirBridge.dir.resolve("settings.json"));
    }
    @LauncherAPI
    @Override
    public NewLauncherSettings getConfig() {
        settings.updatesDirPath = settings.updatesDir.toString();
        return settings;
    }
    @LauncherAPI
    @Override
    public NewLauncherSettings getDefaultConfig() {
        return new NewLauncherSettings();
    }
    @LauncherAPI
    @Override
    public void setConfig(NewLauncherSettings config) {
        settings = config;
        settings.updatesDir = Paths.get(settings.updatesDirPath);
    }

    @Override
    public void setType(Type type) {
        super.setType(type);
    }
}
