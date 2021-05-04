package pro.gravit.launchserver.modules.impl;

import pro.gravit.launcher.LauncherTrustManager;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.impl.SimpleModuleManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Arrays;
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

    @Deprecated
    public void printModulesInfo() {
        for (LauncherModule module : modules) {
            LauncherModuleInfo info = module.getModuleInfo();
            LauncherTrustManager.CheckClassResult checkStatus = module.getCheckResult();
            LogHelper.info("[MODULE] %s v: %s p: %d deps: %s sig: %s", info.name, info.version.getVersionString(), info.priority, Arrays.toString(info.dependencies), checkStatus == null ? "null" : checkStatus.type);
            if (checkStatus != null && checkStatus.endCertificate != null) {
                X509Certificate cert = checkStatus.endCertificate;
                LogHelper.info("[MODULE CERT] Module signer: %s", cert.getSubjectDN().getName());
            }
            if (checkStatus != null && checkStatus.rootCertificate != null) {
                X509Certificate cert = checkStatus.rootCertificate;
                LogHelper.info("[MODULE CERT] Module signer CA: %s", cert.getSubjectDN().getName());
            }
        }
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
