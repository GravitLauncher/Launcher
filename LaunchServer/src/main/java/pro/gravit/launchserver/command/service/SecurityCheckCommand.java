package pro.gravit.launchserver.command.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.protect.AdvancedProtectHandler;
import pro.gravit.launchserver.auth.protect.NoProtectHandler;
import pro.gravit.launchserver.auth.protect.StdProtectHandler;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.components.ProGuardComponent;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.helper.SignHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class SecurityCheckCommand extends Command {
    private static transient final Logger logger = LogManager.getLogger();

    public SecurityCheckCommand(LaunchServer server) {
        super(server);
    }

    public static void printCheckResult(String module, String comment, Boolean status) {
        if (status == null) {
            logger.warn(String.format("[%s] %s", module, comment));
        } else if (status) {
            logger.info(String.format("[%s] %s OK", module, comment));
        } else {
            logger.error(String.format("[%s] %s", module, comment));
        }
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
            boolean bad = false;
            try {
                KeyStore keyStore = SignHelper.getStore(new File(config.sign.keyStore).toPath(), config.sign.keyStorePass, config.sign.keyStoreType);
                Certificate[] certChainPlain = keyStore.getCertificateChain(config.sign.keyAlias);
                List<X509Certificate> certChain = Arrays.stream(certChainPlain).map(e -> (X509Certificate) e).collect(Collectors.toList());
                X509Certificate cert = certChain.get(0);
                cert.checkValidity();
                if (certChain.size() <= 1) {
                    printCheckResult("sign", "certificate chain contains <2 element(recommend 2 and more)", false);
                    bad = true;
                }
                if ((cert.getBasicConstraints() & 1) == 1) {
                    printCheckResult("sign", "end certificate - CA", false);
                    bad = true;
                }
                for (X509Certificate certificate : certChain) {
                    certificate.checkValidity();
                }
            } catch (Throwable e) {
                logger.error("Sign check failed", e);
                bad = true;
            }
            if (!bad)
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
            case DEV -> printCheckResult("env", "found env DEV", false);
            case DEBUG -> printCheckResult("env", "found env DEBUG", false);
            case STD -> printCheckResult("env", "you can improve security by using env PROD", null);
            case PROD -> printCheckResult("env", "", true);
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
                if (Files.exists(server.dir.resolve(".keys")) && checkOtherReadOrWriteAccess(server.dir.resolve(".keys"))) {
                    logger.warn("Write or read access to .keys directory. Please use 'chmod -R 600 .keys'");
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
