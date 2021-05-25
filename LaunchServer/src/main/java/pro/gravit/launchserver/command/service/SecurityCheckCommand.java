package pro.gravit.launchserver.command.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.handler.MemoryAuthHandler;
import pro.gravit.launchserver.auth.protect.AdvancedProtectHandler;
import pro.gravit.launchserver.auth.protect.NoProtectHandler;
import pro.gravit.launchserver.auth.protect.StdProtectHandler;
import pro.gravit.launchserver.auth.provider.AcceptAuthProvider;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.components.ProGuardComponent;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.StringTokenizer;

public class SecurityCheckCommand extends Command {
    private static transient final Logger logger = LogManager.getLogger();

    public SecurityCheckCommand(LaunchServer server) {
        super(server);
    }

    public static void printCheckResult(String module, String comment, Boolean status) {
        logger.info(String.format("[%s] %s - %s", module, comment, status == null ? "WARN" : (status ? "OK" : "FAIL")), false);
    }

    @Deprecated
    public static void printCheckResult(LogHelper.Level level, String module, String comment, Boolean status) {
        printCheckResult(module, comment, status);
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
    public void invoke(String... args) {
        LaunchServerConfig config = server.config;
        config.auth.forEach((name, pair) -> {
            if (pair.provider instanceof AcceptAuthProvider) {
                printCheckResult(String.format("auth.%s.provider", name), "Accept auth provider", false);
            } else {
                printCheckResult(String.format("auth.%s.provider", name), "", true);
            }
            if (pair.handler instanceof MemoryAuthHandler) {
                printCheckResult(String.format("auth.%s.handler", name), "MemoryAuthHandler test-only", false);
            } else {
                printCheckResult(String.format("auth.%s.handler", name), "", true);
            }
        });
        if (config.protectHandler instanceof NoProtectHandler) {
            printCheckResult("protectHandler", "protectHandler none", false);
        } else if (config.protectHandler instanceof AdvancedProtectHandler) {
            printCheckResult("protectHandler", "", true);
            if (!((AdvancedProtectHandler) config.protectHandler).enableHardwareFeature) {
                printCheckResult("protectHandler.hardwareId", "you can improve security by using hwid provider", null);
            } else {
                printCheckResult("protectHandler.hardwareId", "", true);
            }
        } else if (config.protectHandler instanceof StdProtectHandler) {
            printCheckResult("protectHandler", "you can improve security by using advanced", null);
        } else {
            printCheckResult("protectHandler", "unknown protectHandler", null);
        }
        if (config.netty.address.startsWith("ws://")) {
            if (config.netty.ipForwarding)
                printCheckResult("netty.ipForwarding", "ipForwarding may be used to spoofing ip", null);
            printCheckResult("netty.address", "websocket connection not secure", false);
        } else if (config.netty.address.startsWith("wss://")) {
            if (!config.netty.ipForwarding)
                printCheckResult("netty.ipForwarding", "ipForwarding not enabled. authLimiter may be get incorrect ip", null);
            printCheckResult("netty.address", "", true);
        }

        if (config.netty.sendExceptionEnabled) {
            printCheckResult("netty.sendExceptionEnabled", "recommend \"false\" in production", false);
        } else {
            printCheckResult("netty.sendExceptionEnabled", "", true);
        }

        if (config.netty.launcherURL.startsWith("http://")) {
            printCheckResult("netty.launcherUrl", "launcher jar download connection not secure", false);
        } else if (config.netty.launcherURL.startsWith("https://")) {
            printCheckResult("netty.launcherUrl", "", true);
        }

        if (config.netty.launcherEXEURL.startsWith("http://")) {
            printCheckResult("netty.launcherExeUrl", "launcher exe download connection not secure", false);
        } else if (config.netty.launcherEXEURL.startsWith("https://")) {
            printCheckResult("netty.launcherExeUrl", "", true);
        }

        if (config.netty.downloadURL.startsWith("http://")) {
            printCheckResult("netty.downloadUrl", "assets/clients download connection not secure", false);
        } else if (config.netty.downloadURL.startsWith("https://")) {
            printCheckResult("netty.downloadUrl", "", true);
        }

        if (!config.sign.enabled) {
            printCheckResult("sign", "it is recommended to use a signature", null);
        } else {
            /*boolean bad = false;
            KeyStore keyStore = SignHelper.getStore(new File(config.sign.keyStore).toPath(), config.sign.keyStorePass, config.sign.keyStoreType);
            X509Certificate[] certChain = (X509Certificate[]) keyStore.getCertificateChain(config.sign.keyAlias);
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(config.sign.keyAlias);
            cert.checkValidity();
            if(certChain.length <= 1) {
                printCheckResult("sign", "certificate chain contains <2 element(recommend 2 and more)", false);
                bad = true;
            }
            if((cert.getBasicConstraints() & 1) != 0) {
                printCheckResult("sign", "end certificate - CA", false);
                bad = true;
            }
            for(X509Certificate certificate : certChain)
            {
                certificate.checkValidity();
            }
            if(!bad)*/
            printCheckResult("sign", "", true);
        }

        if (config.components.values().stream().noneMatch(c -> c instanceof ProGuardComponent)) {
            printCheckResult("launcher.enabledProGuard", "proguard not enabled", false);
        } else {
            printCheckResult("launcher.enabledProGuard", "", true);
        }
        if (!config.launcher.stripLineNumbers) {
            printCheckResult("launcher.stripLineNumbers", "stripLineNumbers not enabled", false);
        } else {
            printCheckResult("launcher.stripLineNumbers", "", true);
        }

        switch (config.env) {

            case DEV:
                printCheckResult("env", "found env DEV", false);
                break;
            case DEBUG:
                printCheckResult("env", "found env DEBUG", false);
                break;
            case STD:
                printCheckResult("env", "you can improve security by using env PROD", null);
                break;
            case PROD:
                printCheckResult("env", "", true);
                break;
        }

        //Profiles
        for (ClientProfile profile : server.getProfiles()) {
            boolean bad = false;
            String profileModuleName = String.format("profiles.%s", profile.getTitle());
            for (String exc : profile.getUpdateExclusions()) {
                StringTokenizer tokenizer = new StringTokenizer(exc, "/");
                if (exc.endsWith(".jar")) {
                    printCheckResult(profileModuleName, String.format("updateExclusions %s not safe. Cheats may be injected very easy!", exc), false);
                    bad = true;
                    continue;
                }
                if (tokenizer.hasMoreTokens() && tokenizer.nextToken().equals("mods")) {
                    String nextToken = tokenizer.nextToken();
                    if (!tokenizer.hasMoreTokens()) {
                        if (!exc.endsWith("/")) {
                            printCheckResult(profileModuleName, String.format("updateExclusions %s not safe. Cheats may be injected very easy!", exc), false);
                            bad = true;
                        }
                    } else {
                        if (nextToken.equals("memory_repo") || nextToken.equals(profile.getVersion().name)) {
                            printCheckResult(profileModuleName, String.format("updateExclusions %s not safe. Cheats may be injected very easy!", exc), false);
                            bad = true;
                        }
                    }
                }
            }
            if (!bad)
                printCheckResult(profileModuleName, "", true);
        }

        //Linux permissions check
        if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            try {
                int uid = 0, gid = 0;
                String[] status = new String(IOHelper.read(Paths.get("/proc/self/status"))).split("\n");
                for (String line : status) {
                    String[] parts = line.split(":");
                    if (parts.length == 0) continue;
                    if (parts[0].trim().equalsIgnoreCase("Uid")) {
                        String[] words = parts[1].trim().split(" ");
                        uid = Integer.parseInt(words[0]);
                        if (Integer.parseInt(words[0]) == 0 || Integer.parseInt(words[0]) == 0) {
                            logger.error("The process is started as root! It is not recommended");
                        }
                    }
                    if (parts[0].trim().equalsIgnoreCase("Gid")) {
                        String[] words = parts[1].trim().split(" ");
                        gid = Integer.parseInt(words[0]);
                        if (Integer.parseInt(words[0]) == 0 || Integer.parseInt(words[0]) == 0) {
                            logger.error("The process is started as root group! It is not recommended");
                        }
                    }
                }
                if (checkOtherWriteAccess(IOHelper.getCodeSource(LaunchServer.class))) {
                    logger.warn("Write access to LaunchServer.jar. Please use 'chmod 755 LaunchServer.jar'");
                }
                if (Files.exists(server.dir.resolve("private.key")) && checkOtherReadOrWriteAccess(server.dir.resolve("private.key"))) {
                    logger.warn("Write or read access to private.key. Please use 'chmod 600 private.key'");
                }
                if (Files.exists(server.dir.resolve("LaunchServerConfig.json")) && checkOtherReadOrWriteAccess(server.dir.resolve("LaunchServerConfig.json"))) {
                    logger.warn("Write or read access to LaunchServerConfig.json. Please use 'chmod 600 LaunchServerConfig.json'");
                }
                if (Files.exists(server.dir.resolve("LaunchServerRuntimeConfig.json")) && checkOtherReadOrWriteAccess(server.dir.resolve("LaunchServerRuntimeConfig.json"))) {
                    logger.warn("Write or read access to LaunchServerRuntimeConfig.json. Please use 'chmod 600 LaunchServerRuntimeConfig.json'");
                }
            } catch (IOException e) {
                logger.error(e);
            }
        }
        logger.info("Check completed");
    }

    public boolean checkOtherWriteAccess(Path file) throws IOException {
        Set<PosixFilePermission> permissionSet = Files.getPosixFilePermissions(file);
        return permissionSet.contains(PosixFilePermission.OTHERS_WRITE);
    }

    public boolean checkOtherReadOrWriteAccess(Path file) throws IOException {
        Set<PosixFilePermission> permissionSet = Files.getPosixFilePermissions(file);
        return permissionSet.contains(PosixFilePermission.OTHERS_WRITE) || permissionSet.contains(PosixFilePermission.OTHERS_READ);
    }
}
