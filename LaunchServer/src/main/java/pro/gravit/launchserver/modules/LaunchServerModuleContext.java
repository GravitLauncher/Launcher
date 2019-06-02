package pro.gravit.launchserver.modules;

import pro.gravit.launcher.managers.SimpleModulesConfigManager;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launcher.modules.ModulesConfigManager;
import pro.gravit.launcher.modules.ModulesManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.PublicURLClassLoader;

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
