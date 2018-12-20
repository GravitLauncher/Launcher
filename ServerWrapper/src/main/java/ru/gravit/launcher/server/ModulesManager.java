package ru.gravit.launcher.server;

import ru.gravit.launcher.modules.SimpleModuleManager;
import ru.gravit.utils.PublicURLClassLoader;

import java.net.URL;
import java.util.ArrayList;

public class ModulesManager extends SimpleModuleManager {
    public ModulesManager(ServerWrapper wrapper) {
        modules = new ArrayList<>();
        classloader = new PublicURLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        context = new ServerModuleContext(wrapper, classloader);
    }
}
