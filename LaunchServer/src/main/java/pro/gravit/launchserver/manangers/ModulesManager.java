package pro.gravit.launchserver.manangers;

import java.net.URL;
import java.util.ArrayList;

import pro.gravit.launcher.managers.SimpleModuleManager;
import pro.gravit.launcher.managers.SimpleModulesConfigManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.CoreModule;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.PublicURLClassLoader;

public class ModulesManager extends SimpleModuleManager {
    public SimpleModulesConfigManager configManager;

    public ModulesManager(LaunchServer lsrv) {
        modules = new ArrayList<>(1);
        configManager = new SimpleModulesConfigManager(lsrv.dir.resolve("config"));
        classloader = new PublicURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        context = new LaunchServerModuleContext(lsrv, classloader, configManager);
        registerCoreModule();
    }

    private void registerCoreModule() {
        load(new CoreModule());
    }
}
