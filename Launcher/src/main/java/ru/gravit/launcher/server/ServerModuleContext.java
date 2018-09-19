package ru.gravit.launcher.server;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherClassLoader;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesManagerInterface;

public class ServerModuleContext implements ModuleContext {
    public final LauncherClassLoader classLoader;
    public final ServerWrapper wrapper;

    public ServerModuleContext(ServerWrapper wrapper, LauncherClassLoader classLoader) {
        this.classLoader = classLoader;
        this.wrapper = wrapper;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public ModulesManagerInterface getModulesManager() {
        return Launcher.modulesManager;
    }
}
