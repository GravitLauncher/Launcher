package pro.gravit.launcher.base.modules.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.config.SimpleConfigurable;
import pro.gravit.launcher.base.modules.ModulesConfigManager;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleModulesConfigManager implements ModulesConfigManager {

    private static final Logger logger =
            LoggerFactory.getLogger(SimpleModulesConfigManager.class);

    public final Path configDir;

    public SimpleModulesConfigManager(Path configDir) {
        this.configDir = configDir;
    }

    public Path getModuleConfig(String moduleName) {
        return getModuleConfig(moduleName, "Config");
    }

    @Override
    public Path getModuleConfig(String moduleName, String configName) {
        return getModuleConfigDir(moduleName).resolve(configName.concat(".json"));
    }

    public Path getModuleConfigDir(String moduleName) {
        if (!IOHelper.isDir(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                logger.error("", e);
            }
        }
        return configDir.resolve(moduleName);
    }

    @Override
    public <T> SimpleConfigurable<T> getConfigurable(Class<T> tClass, Path configPath) {
        return new SimpleConfigurable<>(tClass, configPath);
    }
}