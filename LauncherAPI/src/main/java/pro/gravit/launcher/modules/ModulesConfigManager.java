package pro.gravit.launcher.modules;

import pro.gravit.launcher.config.SimpleConfigurable;

import java.nio.file.Path;

public interface ModulesConfigManager {
    Path getModuleConfig(String moduleName);

    Path getModuleConfig(String moduleName, String configName);

    Path getModuleConfigDir(String moduleName);

    <T> SimpleConfigurable<T> getConfigurable(Class<T> tClass, Path configPath);

    default <T> SimpleConfigurable<T> getConfigurable(Class<T> tClass, String moduleName) {
        return getConfigurable(tClass, getModuleConfig(moduleName));
    }

    default <T> SimpleConfigurable<T> getConfigurable(Class<T> tClass, String moduleName, String configName) {
        return getConfigurable(tClass, getModuleConfig(moduleName, configName));
    }
}
