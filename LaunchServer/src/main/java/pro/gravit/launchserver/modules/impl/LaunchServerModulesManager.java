package pro.gravit.launchserver.modules.impl;

import pro.gravit.launcher.LauncherTrustManager;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.impl.SimpleModuleManager;
import pro.gravit.launchserver.LaunchServer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class LaunchServerModulesManager extends SimpleModuleManager {
    public final LaunchServerCoreModule coreModule;

    public LaunchServerModulesManager(Path modulesDir, Path configDir, LauncherTrustManager trustManager) {
        super(modulesDir, configDir, trustManager);
        coreModule = new LaunchServerCoreModule();
        loadModule(coreModule);
    }

    public void fullInitializedLaunchServer(LaunchServer server) {
        initContext = new LaunchServerInitContext(server);
    }

    public List<LauncherModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    @Override
    public final boolean verifyClassCheckResult(LauncherTrustManager.CheckClassResult result) {
        return true;
    }

    @Override
    public LauncherModule getCoreModule() {
        return coreModule;
    }
}
