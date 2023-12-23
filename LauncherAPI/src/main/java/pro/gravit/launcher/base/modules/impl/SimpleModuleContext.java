package pro.gravit.launcher.base.modules.impl;

import pro.gravit.launcher.base.modules.LauncherModulesContext;
import pro.gravit.launcher.base.modules.LauncherModulesManager;
import pro.gravit.launcher.base.modules.ModulesConfigManager;

public class SimpleModuleContext implements LauncherModulesContext {
    public final LauncherModulesManager modulesManager;
    public final ModulesConfigManager configManager;

    public SimpleModuleContext(LauncherModulesManager modulesManager, ModulesConfigManager configManager) {
        this.modulesManager = modulesManager;
        this.configManager = configManager;
    }

    @Override
    public LauncherModulesManager getModulesManager() {
        return modulesManager;
    }

    @Override
    public ModulesConfigManager getModulesConfigManager() {
        return configManager;
    }
}
