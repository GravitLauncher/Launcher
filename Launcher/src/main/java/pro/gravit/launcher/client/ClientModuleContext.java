package pro.gravit.launcher.client;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launcher.modules.ModulesConfigManager;
import pro.gravit.launcher.modules.ModulesManager;

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
    public ModulesManager getModulesManager() {
        return Launcher.modulesManager;
    }

    @Override
    public ModulesConfigManager getModulesConfigManager() {
        return null; // ClientModuleContext не поддерживает modulesConfigManager
    }
}
