package ru.gravit.launcher.client;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherEngine;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesConfigManagerInterface;
import ru.gravit.launcher.modules.ModulesManagerInterface;

public class ClientModuleContext implements ModuleContext {
    public final LauncherEngine engine;

    ClientModuleContext(LauncherEngine engine) {
        this.engine = engine;
    }

    @Override
    public Type getType() {
        return Type.CLIENT;
    }

    @Override
    public ModulesManagerInterface getModulesManager() {
        return Launcher.modulesManager;
    }

    @Override
    public ModulesConfigManagerInterface getModulesConfigManager() {
        return null; // ClientModuleContext не поддерживает modulesConfigManager
    }
}
