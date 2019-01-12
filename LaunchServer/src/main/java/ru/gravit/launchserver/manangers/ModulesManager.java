package ru.gravit.launchserver.manangers;

import java.net.URL;
import java.util.ArrayList;

import ru.gravit.launcher.managers.ModulesConfigManager;
import ru.gravit.launcher.managers.SimpleModuleManager;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.modules.CoreModule;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;
import ru.gravit.utils.PublicURLClassLoader;

public class ModulesManager extends SimpleModuleManager {
    public ModulesConfigManager configManager;
    public ModulesManager(LaunchServer lsrv) {
        modules = new ArrayList<>(1);
        configManager = new ModulesConfigManager(lsrv.dir.resolve("config"));
        classloader = new PublicURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        context = new LaunchServerModuleContext(lsrv, classloader, configManager);
        registerCoreModule();
    }

    private void registerCoreModule() {
        load(new CoreModule());
    }
}
