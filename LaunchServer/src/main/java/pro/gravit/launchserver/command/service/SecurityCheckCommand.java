package pro.gravit.launchserver.command.service;

import org.fusesource.jansi.Ansi;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.handler.MemoryAuthHandler;
import pro.gravit.launchserver.auth.protect.AdvancedProtectHandler;
import pro.gravit.launchserver.auth.protect.NoProtectHandler;
import pro.gravit.launchserver.auth.protect.StdProtectHandler;
import pro.gravit.launchserver.auth.provider.AcceptAuthProvider;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.utils.helper.FormatHelper;
import pro.gravit.utils.helper.LogHelper;

import java.util.StringTokenizer;

public class SecurityCheckCommand extends Command {
    public SecurityCheckCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "check configuration";
    }

    @Override
    public void invoke(String... args) throws Exception {
        LaunchServerConfig config = server.config;
        config.auth.forEach((name, pair) -> {
            if (pair.provider instanceof AcceptAuthProvider) {
                printCheckResult(LogHelper.Level.INFO, String.format("auth.%s.provider", name), "Accept auth provider", false);
            } else {
                printCheckResult(LogHelper.Level.INFO, String.format("auth.%s.provider", name), "", true);
            }
            if (pair.handler instanceof MemoryAuthHandler) {
                printCheckResult(LogHelper.Level.INFO, String.format("auth.%s.handler", name), "MemoryAuthHandler test-only", false);
            } else {
                printCheckResult(LogHelper.Level.INFO, String.format("auth.%s.handler", name), "", true);
            }
        });
        if (config.protectHandler instanceof NoProtectHandler) {
            printCheckResult(LogHelper.Level.INFO, "protectHandler", "protectHandler none", false);
        } else if (config.protectHandler instanceof AdvancedProtectHandler) {
            printCheckResult(LogHelper.Level.INFO, "protectHandler", "", true);
            if (!((AdvancedProtectHandler) config.protectHandler).enableHardwareFeature) {
                printCheckResult(LogHelper.Level.INFO, "protectHandler.hardwareId", "you can improve security by using hwid provider", null);
            } else {
                printCheckResult(LogHelper.Level.INFO, "protectHandler.hardwareId", "", true);
            }
        } else if (config.protectHandler instanceof StdProtectHandler) {
            printCheckResult(LogHelper.Level.INFO, "protectHandler", "you can improve security by using advanced", null);
        } else {
            printCheckResult(LogHelper.Level.INFO, "protectHandler", "unknown protectHandler", null);
        }
        if (config.netty.address.startsWith("ws://")) {
            if (config.netty.ipForwarding)
                printCheckResult(LogHelper.Level.INFO, "netty.ipForwarding", "ipForwarding may be used to spoofing ip", null);
            printCheckResult(LogHelper.Level.INFO, "netty.address", "websocket connection not secure", false);
        } else if (config.netty.address.startsWith("wss://")) {
            if (!config.netty.ipForwarding)
                printCheckResult(LogHelper.Level.INFO, "netty.ipForwarding", "ipForwarding not enabled. authLimiter may be get incorrect ip", null);
            printCheckResult(LogHelper.Level.INFO, "netty.address", "", true);
        }

        if (config.netty.sendExceptionEnabled) {
            printCheckResult(LogHelper.Level.INFO, "netty.sendExceptionEnabled", "recommend \"false\" in production", false);
        } else {
            printCheckResult(LogHelper.Level.INFO, "netty.sendExceptionEnabled", "", true);
        }

        if (config.netty.launcherURL.startsWith("http://")) {
            printCheckResult(LogHelper.Level.INFO, "netty.launcherUrl", "launcher jar download connection not secure", false);
        } else if (config.netty.launcherURL.startsWith("https://")) {
            printCheckResult(LogHelper.Level.INFO, "netty.launcherUrl", "", true);
        }

        if (config.netty.launcherEXEURL.startsWith("http://")) {
            printCheckResult(LogHelper.Level.INFO, "netty.launcherExeUrl", "launcher exe download connection not secure", false);
        } else if (config.netty.launcherEXEURL.startsWith("https://")) {
            printCheckResult(LogHelper.Level.INFO, "netty.launcherExeUrl", "", true);
        }

        if (config.netty.downloadURL.startsWith("http://")) {
            printCheckResult(LogHelper.Level.INFO, "netty.downloadUrl", "assets/clients download connection not secure", false);
        } else if (config.netty.downloadURL.startsWith("https://")) {
            printCheckResult(LogHelper.Level.INFO, "netty.downloadUrl", "", true);
        }

        if (!config.sign.enabled) {
            printCheckResult(LogHelper.Level.INFO, "sign", "it is recommended to use a signature", null);
        } else {
            /*boolean bad = false;
            KeyStore keyStore = SignHelper.getStore(new File(config.sign.keyStore).toPath(), config.sign.keyStorePass, config.sign.keyStoreType);
            X509Certificate[] certChain = (X509Certificate[]) keyStore.getCertificateChain(config.sign.keyAlias);
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(config.sign.keyAlias);
            cert.checkValidity();
            if(certChain.length <= 1) {
                printCheckResult(LogHelper.Level.INFO, "sign", "certificate chain contains <2 element(recommend 2 and more)", false);
                bad = true;
            }
            if((cert.getBasicConstraints() & 1) != 0) {
                printCheckResult(LogHelper.Level.INFO, "sign", "end certificate - CA", false);
                bad = true;
            }
            for(X509Certificate certificate : certChain)
            {
                certificate.checkValidity();
            }
            if(!bad)*/
            printCheckResult(LogHelper.Level.INFO, "sign", "", true);
        }

        if (!config.launcher.enabledProGuard) {
            printCheckResult(LogHelper.Level.INFO, "launcher.enabledProGuard", "proguard not enabled", false);
        } else {
            printCheckResult(LogHelper.Level.INFO, "launcher.enabledProGuard", "", true);
        }
        if (!config.launcher.stripLineNumbers) {
            printCheckResult(LogHelper.Level.INFO, "launcher.stripLineNumbers", "stripLineNumbers not enabled", false);
        } else {
            printCheckResult(LogHelper.Level.INFO, "launcher.stripLineNumbers", "", true);
        }

        switch (config.env) {

            case DEV:
                printCheckResult(LogHelper.Level.INFO, "env", "found env DEV", false);
                break;
            case DEBUG:
                printCheckResult(LogHelper.Level.INFO, "env", "found env DEBUG", false);
                break;
            case STD:
                printCheckResult(LogHelper.Level.INFO, "env", "you can improve security by using env PROD", null);
                break;
            case PROD:
                printCheckResult(LogHelper.Level.INFO, "env", "", true);
                break;
        }

        //Profiles
        for (ClientProfile profile : server.getProfiles()) {
            boolean bad = false;
            String profileModuleName = String.format("profiles.%s", profile.getTitle());
            for (String exc : profile.getUpdateExclusions()) {
                StringTokenizer tokenizer = new StringTokenizer(exc, "/");
                if (exc.endsWith(".jar")) {
                    printCheckResult(LogHelper.Level.INFO, profileModuleName, String.format("updateExclusions %s not safe. Cheats may be injected very easy!", exc), false);
                    bad = true;
                    continue;
                }
                if (tokenizer.hasMoreTokens() && tokenizer.nextToken().equals("mods")) {
                    String nextToken = tokenizer.nextToken();
                    if (!tokenizer.hasMoreTokens()) {
                        if(!exc.endsWith("/")) {
                            printCheckResult(LogHelper.Level.INFO, profileModuleName, String.format("updateExclusions %s not safe. Cheats may be injected very easy!", exc), false);
                            bad = true;
                        }
                    } else {
                        if (nextToken.equals("memory_repo") || nextToken.equals(profile.getVersion().name)) {
                            printCheckResult(LogHelper.Level.INFO, profileModuleName, String.format("updateExclusions %s not safe. Cheats may be injected very easy!", exc), false);
                            bad = true;
                        }
                    }
                }
            }
            if (!bad)
                printCheckResult(LogHelper.Level.INFO, profileModuleName, "", true);
        }
        LogHelper.info("Check completed");
    }

    public static void printCheckResult(LogHelper.Level level, String module, String comment, Boolean status) {
        LogHelper.rawLog(() -> FormatHelper.rawFormat(level, LogHelper.getDataTime(), false).concat(String.format("[%s] %s - %s", module, comment, status == null ? "WARN" : (status ? "OK" : "FAIL"))),
                () -> FormatHelper.rawAnsiFormat(level, LogHelper.getDataTime(), false)
                        .fgBright(Ansi.Color.WHITE)
                        .a("[")
                        .fgBright(Ansi.Color.BLUE)
                        .a(module)
                        .fgBright(Ansi.Color.WHITE)
                        .a("] ".concat(comment).concat(" - "))
                        .fgBright(status == null ? Ansi.Color.YELLOW : (status ? Ansi.Color.GREEN : Ansi.Color.RED))
                        .a(status == null ? "WARN" : (status ? "OK" : "FAIL"))
                        .reset().toString());
    }
}
