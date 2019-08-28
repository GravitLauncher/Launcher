package pro.gravit.launcher.managers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import pro.gravit.launcher.modules.ModulesConfigManager;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class SimpleModulesConfigManager implements ModulesConfigManager {
    public Path configDir;

    public SimpleModulesConfigManager(Path configDir) {
        this.configDir = configDir;
    }

    public Path getModuleConfig(String moduleName) {
        return getModuleConfig(moduleName, "Config");
    }

    @Override
    public Path getModuleConfig(String moduleName, String configName) {
        return getModuleConfigDir(moduleName).resolve(moduleName.concat(configName.concat(".json")));
    }

    public Path getModuleConfigDir(String moduleName) {
        if (!IOHelper.isDir(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        return configDir.resolve(moduleName);
    }
}
