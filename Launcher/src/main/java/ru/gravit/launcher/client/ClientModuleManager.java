package ru.gravit.launcher.client;

import java.net.URL;
import java.util.ArrayList;

import ru.gravit.launcher.LauncherEngine;
import ru.gravit.launcher.modules.SimpleModuleManager;

public class ClientModuleManager extends SimpleModuleManager {
    public ClientModuleManager(LauncherEngine engine)
    {
        context = new ClientModuleContext(engine);
        modules = new ArrayList<>();
    }

    @Override
    public void loadModule(URL jarpath, boolean preload) {
        throw new SecurityException("Custom JAR's load not allowed here");
    }

    @Override
    public void loadModule(URL jarpath, String classname, boolean preload) {
        throw new SecurityException("Custom JAR's load not allowed here");
    }
}
