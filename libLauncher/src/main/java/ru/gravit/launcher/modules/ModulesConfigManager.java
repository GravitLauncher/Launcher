package ru.gravit.launcher.modules;

import java.nio.file.Path;

public interface ModulesConfigManager {
    Path getModuleConfig(String moduleName);

    Path getModuleConfigDir(String moduleName);
}
