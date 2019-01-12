package ru.gravit.launcher.server;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.managers.ModulesConfigManager;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesConfigManagerInterface;
import ru.gravit.launcher.modules.ModulesManagerInterface;
import ru.gravit.utils.PublicURLClassLoader;

public class ServerModuleContext implements ModuleContext {
    public final PublicURLClassLoader classLoader;
    public final ServerWrapper wrapper;
    public final ModulesConfigManager modulesConfigManager;

    public ServerModuleContext(ServerWrapper wrapper, PublicURLClassLoader classLoader, ModulesConfigManager modulesConfigManager) {
        this.classLoader = classLoader;
        this.wrapper = wrapper;
        this.modulesConfigManager = modulesConfigManager;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public ModulesManagerInterface getModulesManager() {
        return Launcher.modulesManager;
    }

    @Override
    public ModulesConfigManagerInterface getModulesConfigManager() {
        return modulesConfigManager;
    }
}
