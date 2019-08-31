package pro.gravit.launcher.modules;

import java.nio.file.Path;

public interface ModulesConfigManager {
    Path getModuleConfig(String moduleName);

    Path getModuleConfig(String moduleName, String configName);

    Path getModuleConfigDir(String moduleName);
}
