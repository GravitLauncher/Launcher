package ru.gravit.launcher.server;

import java.net.URL;
import java.util.ArrayList;

import ru.gravit.utils.PublicURLClassLoader;
import ru.gravit.launcher.modules.SimpleModuleManager;

public class ModulesManager extends SimpleModuleManager {
    public ModulesManager(ServerWrapper wrapper) {
        modules = new ArrayList<>();
        classloader = new PublicURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        context = new ServerModuleContext(wrapper, classloader);
    }
}
