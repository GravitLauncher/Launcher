package ru.gravit.launchserver.modules;

import ru.gravit.launcher.managers.SimpleModulesConfigManager;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesConfigManager;
import ru.gravit.launcher.modules.ModulesManager;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.PublicURLClassLoader;

public class LaunchServerModuleContext implements ModuleContext {
    public final LaunchServer launchServer;
    public final PublicURLClassLoader classloader;
    public final SimpleModulesConfigManager modulesConfigManager;

    public LaunchServerModuleContext(LaunchServer server, PublicURLClassLoader classloader, SimpleModulesConfigManager modulesConfigManager) {
        launchServer = server;
        this.classloader = classloader;
        this.modulesConfigManager = modulesConfigManager;
    }

    @Override
    public Type getType() {
        return Type.LAUNCHSERVER;
    }

    @Override
    public ModulesManager getModulesManager() {
        return launchServer.modulesManager;
    }

    @Override
    public ModulesConfigManager getModulesConfigManager() {
        return modulesConfigManager;
    }
}
