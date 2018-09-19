package ru.gravit.launchserver.modules;

import ru.gravit.launcher.LauncherClassLoader;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesManagerInterface;
import ru.gravit.launchserver.LaunchServer;

public class LaunchServerModuleContext implements ModuleContext {
    public final LaunchServer launchServer;
    public final LauncherClassLoader classloader;
    public LaunchServerModuleContext(LaunchServer server, LauncherClassLoader classloader)
    {
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
