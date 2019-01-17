package ru.gravit.launcher.client;

import ru.gravit.launcher.LauncherEngine;
import ru.gravit.launcher.managers.SimpleModuleManager;

import java.net.URL;
import java.util.ArrayList;

public class ClientModuleManager extends SimpleModuleManager {
    public ClientModuleManager(LauncherEngine engine) {
        context = new ClientModuleContext(engine);
        modules = new ArrayList<>();
    }

    @Override
    public void loadModule(URL jarpath, String classname) {
        throw new SecurityException("Custom JAR's load not allowed here");
    }

    @Override
    public void loadModuleFull(URL jarpath) {
        throw new SecurityException("Custom JAR's load not allowed here");
    }
}
