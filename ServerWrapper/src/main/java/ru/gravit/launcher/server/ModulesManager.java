package ru.gravit.launcher.server;

import ru.gravit.launcher.managers.ModulesConfigManager;
import ru.gravit.launcher.managers.SimpleModuleManager;
import ru.gravit.utils.PublicURLClassLoader;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ModulesManager extends SimpleModuleManager {
    public ModulesConfigManager modulesConfigManager;
    public ModulesManager(ServerWrapper wrapper) {
        modules = new ArrayList<>();
        modulesConfigManager = new ModulesConfigManager(Paths.get("modules-config"));
        classloader = new PublicURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        context = new ServerModuleContext(wrapper, classloader, modulesConfigManager);
    }
}
