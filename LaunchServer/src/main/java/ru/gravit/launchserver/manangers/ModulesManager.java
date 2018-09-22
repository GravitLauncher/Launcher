package ru.gravit.launchserver.manangers;

import java.net.URL;
import java.util.ArrayList;

import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.launcher.modules.SimpleModuleManager;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.modules.CoreModule;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;

public class ModulesManager extends SimpleModuleManager {
    public ModulesManager(LaunchServer lsrv) {
        modules = new ArrayList<>(1);
        classloader = new PublicURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        context = new LaunchServerModuleContext(lsrv, classloader);
    }

    private void registerCoreModule() {
        load(new CoreModule());
    }
}
