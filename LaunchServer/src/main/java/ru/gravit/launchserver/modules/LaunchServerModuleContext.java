package ru.gravit.launchserver.modules;

import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesManagerInterface;
import ru.gravit.launchserver.LaunchServer;

public class LaunchServerModuleContext implements ModuleContext {
    public final LaunchServer launchServer;
    public final PublicURLClassLoader classloader;

    public LaunchServerModuleContext(LaunchServer server, PublicURLClassLoader classloader) {
        launchServer = server;
        this.classloader = classloader;
    }

    @Override
    public Type getType() {
        return Type.LAUNCHSERVER;
    }

    @Override
    public ModulesManagerInterface getModulesManager() {
        return launchServer.modulesManager;
    }
}
