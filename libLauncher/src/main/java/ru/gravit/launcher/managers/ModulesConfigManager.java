package ru.gravit.launcher.managers;

import ru.gravit.launcher.modules.ModulesConfigManagerInterface;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModulesConfigManager implements ModulesConfigManagerInterface {
    public Path configDir;

    public ModulesConfigManager(Path configDir) {
        this.configDir = configDir;
    }

    public Path getModuleConfig(String moduleName)
    {
        if(!IOHelper.isDir(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        return configDir.resolve(moduleName.concat("Config.json"));
    }
    public Path getModuleConfigDir(String moduleName)
    {
        if(!IOHelper.isDir(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        return configDir.resolve(moduleName);
    }
}
