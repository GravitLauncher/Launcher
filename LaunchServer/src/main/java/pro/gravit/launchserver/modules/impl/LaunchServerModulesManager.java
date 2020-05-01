package pro.gravit.launchserver.modules.impl;

import pro.gravit.launcher.LauncherTrustManager;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.impl.SimpleModuleManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.util.Arrays;

public class LaunchServerModulesManager extends SimpleModuleManager {
    public final LaunchServerCoreModule coreModule;

    public LaunchServerModulesManager(Path modulesDir, Path configDir, LauncherTrustManager trustManager) {
        super(modulesDir, configDir, trustManager);
        coreModule = new LaunchServerCoreModule();
        checkMode = LauncherTrustManager.CheckMode.NONE_IN_NOT_SIGNED;
        loadModule(coreModule);
    }

    public void fullInitializedLaunchServer(LaunchServer server) {
        initContext = new LaunchServerInitContext(server);
    }

    public void printModulesInfo() {
        for (LauncherModule module : modules) {
            LauncherModuleInfo info = module.getModuleInfo();
            LogHelper.info("[MODULE] %s v: %s p: %d deps: %s", info.name, info.version.getVersionString(), info.priority, Arrays.toString(info.dependencies));
        }
    }

    @Override
    public LauncherModule getCoreModule() {
        return coreModule;
    }
}
