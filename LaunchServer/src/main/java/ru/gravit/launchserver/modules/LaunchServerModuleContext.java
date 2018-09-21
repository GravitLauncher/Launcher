package ru.gravit.launchserver.modules;

import ru.gravit.launcher.LauncherClassLoader;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.modules.ModulesManagerInterface;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.asm.ClassMetadataReader;

public class LaunchServerModuleContext implements ModuleContext {
    public final LaunchServer launchServer;
    public final LauncherClassLoader classloader;
	public final ClassMetadataReader metadataReader;
	
    public LaunchServerModuleContext(LaunchServer server, LauncherClassLoader classloader, ClassMetadataReader metadataReader)
    {
        launchServer = server;
        this.classloader = classloader;
        this.metadataReader = metadataReader;
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
