package pro.gravit.launcher.server;

import pro.gravit.launcher.managers.SimpleModulesConfigManager;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launcher.modules.ModulesConfigManager;
import pro.gravit.launcher.modules.ModulesManager;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.launcher.Launcher;

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
