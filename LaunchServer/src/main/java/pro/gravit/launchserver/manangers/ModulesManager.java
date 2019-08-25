package pro.gravit.launchserver.manangers;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;

import pro.gravit.launcher.managers.SimpleModuleManager;
import pro.gravit.launcher.managers.SimpleModulesConfigManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.CoreModule;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.PublicURLClassLoader;

public class ModulesManager extends SimpleModuleManager {
    public SimpleModulesConfigManager configManager;

    public ModulesManager(Path configDir) {
        modules = new ArrayList<>(1);
        configManager = new SimpleModulesConfigManager(configDir);
        classloader = new PublicURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        registerCoreModule();
    }

    public void initContext(LaunchServer server)
    {
        context = new LaunchServerModuleContext(server, classloader, configManager);
    }

    private void registerCoreModule() {
        load(new CoreModule());
    }
}
