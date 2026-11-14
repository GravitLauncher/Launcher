package pro.gravit.launcher.runtime.backend;

import org.slf4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.runtime.client.DirBridge;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.JavaHelper;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers bundled Java runtimes and, when allowed, system Java installations. */
final class JavaRuntimeService {
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile(
            "Java (?<version>.+) b(?<build>.+) (?<os>.+) (?<arch>.+) javafx (?<javafx>.+)");
    private final Logger logger;

    JavaRuntimeService(Logger logger) {
        this.logger = logger;
    }

    List<LauncherBackendAPI.Java> discover() {
        List<LauncherBackendAPI.Java> versions = customRuntimes();
        if (!Launcher.getConfig().forceUseCustomJava || versions.isEmpty()) {
            versions.addAll(JavaHelper.findJava());
        }
        return List.copyOf(versions);
    }

    List<LauncherBackendAPI.Java> customRuntimes() {
        List<LauncherBackendAPI.Java> versions = new ArrayList<>();
        Map<String, String> configured = Launcher.getConfig().customJavaDownload;
        if (configured == null) return versions;
        for (var entry : configured.entrySet()) {
            Matcher matcher = JAVA_VERSION_PATTERN.matcher(entry.getValue());
            if (!matcher.matches()) {
                logger.warn("Java Version: {} does not match", entry.getValue());
                continue;
            }
            String os = matcher.group("os");
            int version = Integer.parseInt(matcher.group("version"));
            int build = Integer.parseInt(matcher.group("build"));
            JVMHelper.ARCH arch = JVMHelper.ARCH.valueOf(matcher.group("arch"));
            boolean javafx = Boolean.parseBoolean(matcher.group("javafx"));
            if (!isArchAvailable(arch) || !JVMHelper.OS_TYPE.name.equals(os)) continue;
            Path javaDirectory = DirBridge.dirUpdates.resolve(entry.getKey());
            logger.debug("In-Launcher Java Version found: Java {} b{} {} javafx {}", version, build, arch.name, Boolean.toString(javafx));
            versions.add(new JavaHelper.JavaVersion(javaDirectory, version, build, arch, javafx));
        }
        return versions;
    }

    boolean isArchAvailable(JVMHelper.ARCH arch) {
        if (JVMHelper.ARCH_TYPE == arch) return true;
        if (arch == JVMHelper.ARCH.X86_64 && JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE
                && ((JVMHelper.ARCH_TYPE == JVMHelper.ARCH.X86 && !JVMHelper.isJVMMatchesSystemArch())
                || JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64)) return true;
        return arch == JVMHelper.ARCH.X86_64 && JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX
                && JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64;
    }
}
