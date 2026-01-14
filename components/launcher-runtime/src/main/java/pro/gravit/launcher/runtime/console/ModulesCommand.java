package pro.gravit.launcher.runtime.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger =
            LoggerFactory.getLogger(ModulesCommand.class);

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
                logger.info("[MODULE] {} v: {}", info.name, info.version.getVersionString());
            } else {
                logger.info("[MODULE] {} v: {} p: {} deps: {} sig: {}", info.name, info.version.getVersionString(), info.priority, Arrays.toString(info.dependencies), checkStatus == null ? "null" : checkStatus.type);
                printCheckStatusInfo(checkStatus);
            }
        }
    }

    private void printCheckStatusInfo(LauncherTrustManager.CheckClassResult checkStatus) {
        if (checkStatus != null && checkStatus.endCertificate != null) {
            X509Certificate cert = checkStatus.endCertificate;
            logger.info("[MODULE CERT] Module signer: {}", cert.getSubjectX500Principal().getName());
        }
        if (checkStatus != null && checkStatus.rootCertificate != null) {
            X509Certificate cert = checkStatus.rootCertificate;
            logger.info("[MODULE CERT] Module signer CA: {}", cert.getSubjectX500Principal().getName());
        }
    }
}