package ru.gravit.launcher.modules;

import java.nio.file.Path;

public interface ModulesConfigManagerInterface {
    Path getModuleConfig(String moduleName);

    Path getModuleConfigDir(String moduleName);
}
