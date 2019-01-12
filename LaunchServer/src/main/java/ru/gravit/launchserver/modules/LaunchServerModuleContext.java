package ru.gravit.launchserver.modules;

import ru.gravit.launcher.managers.ModulesConfigManager;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesConfigManagerInterface;
import ru.gravit.launcher.modules.ModulesManagerInterface;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.PublicURLClassLoader;

public class LaunchServerModuleContext implements ModuleContext {
    public final LaunchServer launchServer;
    public final PublicURLClassLoader classloader;
    public final ModulesConfigManager modulesConfigManager;

    public LaunchServerModuleContext(LaunchServer server, PublicURLClassLoader classloader, ModulesConfigManager modulesConfigManager) {
        launchServer = server;
        this.classloader = classloader;
        this.modulesConfigManager = modulesConfigManager;
    }

    @Override
    public Type getType() {
        return Type.LAUNCHSERVER;
    }

    @Override
    public ModulesManagerInterface getModulesManager() {
        return launchServer.modulesManager;
    }

    @Override
    public ModulesConfigManagerInterface getModulesConfigManager() {
        return modulesConfigManager;
    }
}
