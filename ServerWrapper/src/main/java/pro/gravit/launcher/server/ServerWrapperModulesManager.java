package pro.gravit.launcher.server;

import java.nio.file.Path;

import pro.gravit.launcher.modules.impl.SimpleModuleManager;

public class ServerWrapperModulesManager extends SimpleModuleManager {
    public ServerWrapperModulesManager(Path modulesDir, Path configDir) {
        super(modulesDir, configDir);
    }
    public void fullInitializeServerWrapper(ServerWrapper serverWrapper)
    {
        initContext = new ServerWrapperInitContext(serverWrapper);
    }
}
