package ru.gravit.launcher.server;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.managers.SimpleModulesConfigManager;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesConfigManager;
import ru.gravit.launcher.modules.ModulesManager;
import ru.gravit.utils.PublicURLClassLoader;

public class ServerModuleContext implements ModuleContext {
    public final PublicURLClassLoader classLoader;
    public final ServerWrapper wrapper;
    public final SimpleModulesConfigManager modulesConfigManager;

    public ServerModuleContext(ServerWrapper wrapper, PublicURLClassLoader classLoader, SimpleModulesConfigManager modulesConfigManager) {
        this.classLoader = classLoader;
        this.wrapper = wrapper;
        this.modulesConfigManager = modulesConfigManager;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public ModulesManager getModulesManager() {
        return Launcher.modulesManager;
    }

    @Override
    public ModulesConfigManager getModulesConfigManager() {
        return modulesConfigManager;
    }
}
