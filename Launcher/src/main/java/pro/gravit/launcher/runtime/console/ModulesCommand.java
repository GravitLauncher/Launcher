package pro.gravit.launcher.runtime.console;

import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.runtime.managers.ConsoleManager;
import pro.gravit.launcher.core.LauncherTrustManager;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

import java.security.cert.X509Certificate;
import java.util.Arrays;

public class ModulesCommand extends Command {
    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "show modules";
    }

    @Override
    public void invoke(String... args) {
        for (LauncherModule module : LauncherEngine.modulesManager.getModules()) {
            LauncherModuleInfo info = module.getModuleInfo();
            LauncherTrustManager.CheckClassResult checkStatus = module.getCheckResult();
            if (!ConsoleManager.isConsoleUnlock) {
                LogHelper.info("[MODULE] %s v: %s", info.name, info.version.getVersionString());
            } else {
                LogHelper.info("[MODULE] %s v: %s p: %d deps: %s sig: %s", info.name, info.version.getVersionString(), info.priority, Arrays.toString(info.dependencies), checkStatus == null ? "null" : checkStatus.type);
                printCheckStatusInfo(checkStatus);
            }
        }
    }

    private void printCheckStatusInfo(LauncherTrustManager.CheckClassResult checkStatus) {
        if (checkStatus != null && checkStatus.endCertificate != null) {
            X509Certificate cert = checkStatus.endCertificate;
            LogHelper.info("[MODULE CERT] Module signer: %s", cert.getSubjectX500Principal().getName());
        }
        if (checkStatus != null && checkStatus.rootCertificate != null) {
            X509Certificate cert = checkStatus.rootCertificate;
            LogHelper.info("[MODULE CERT] Module signer CA: %s", cert.getSubjectX500Principal().getName());
        }
    }
}
