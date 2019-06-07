package pro.gravit.launcher.client;

import java.net.URL;
import java.util.ArrayList;

import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.managers.SimpleModuleManager;

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
