package pro.gravit.launcher;

import pro.gravit.launcher.client.ClientModuleManager;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.utils.DirWatcher;
import pro.gravit.utils.helper.EnvHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class ClientLauncherWrapper {
    public static final String MAGIC_ARG = "-Djdk.attach.allowAttachSelf";
    public static final String WAIT_PROCESS_PROPERTY = "launcher.waitProcess";
    public static final String NO_JAVA_CHECK_PROPERTY = "launcher.noJavaCheck";
    public static boolean noJavaCheck = Boolean.getBoolean(NO_JAVA_CHECK_PROPERTY);
    public static boolean waitProcess = Boolean.getBoolean(WAIT_PROCESS_PROPERTY);
    @LauncherInject("launcher.memory")
    public static int launcherMemoryLimit;

    public static class JavaVersion {
        public final Path jvmDir;
        public final int version;
        public boolean enabledJavaFX;

        public JavaVersion(Path jvmDir, int version) {
            this.jvmDir = jvmDir;
            this.version = version;
            this.enabledJavaFX = true;
        }

        public static JavaVersion getCurrentJavaVersion() {
            return new JavaVersion(Paths.get(System.getProperty("java.home")), JVMHelper.getVersion());
        }

        public static JavaVersion getByPath(Path jvmDir) throws IOException {
            Path releaseFile = jvmDir.resolve("release");
            if (!IOHelper.isFile(releaseFile)) return null;
            Properties properties = new Properties();
            properties.load(IOHelper.newReader(releaseFile));
            int javaVersion = getJavaVersion(properties.getProperty("JAVA_VERSION").replaceAll("\"", ""));
            JavaVersion resultJavaVersion = new JavaVersion(jvmDir, javaVersion);
            if (javaVersion <= 8) {
                resultJavaVersion.enabledJavaFX = isExistExtJavaLibrary(jvmDir, "jfxrt");
            } else {
                resultJavaVersion.enabledJavaFX = tryFindModule(jvmDir, "javafx.base") != null;
                if (!resultJavaVersion.enabledJavaFX)
                    resultJavaVersion.enabledJavaFX = tryFindModule(jvmDir.resolve("jre"), "javafx.base") != null;
            }
            return resultJavaVersion;
        }

        public static boolean isExistExtJavaLibrary(Path jvmDir, String name) {
            Path jrePath = jvmDir.resolve("lib").resolve("ext").resolve(name.concat(".jar"));
            Path jdkPath = jvmDir.resolve("jre").resolve("lib").resolve("ext").resolve(name.concat(".jar"));
            return IOHelper.isFile(jrePath) || IOHelper.isFile(jdkPath);
        }
    }

    public static void main(String[] arguments) throws IOException, InterruptedException {
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        JVMHelper.checkStackTrace(ClientLauncherWrapper.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParams();
        LauncherConfig config = Launcher.getConfig();
        LauncherEngine.modulesManager = new ClientModuleManager();
        LauncherConfig.initModules(LauncherEngine.modulesManager);

        LogHelper.info("Launcher for project %s", config.projectName);
        if (config.environment.equals(LauncherConfig.LauncherEnvironment.PROD)) {
            if (System.getProperty(LogHelper.DEBUG_PROPERTY) != null) {
                LogHelper.warning("Found -Dlauncher.debug=true");
            }
            if (System.getProperty(LogHelper.STACKTRACE_PROPERTY) != null) {
                LogHelper.warning("Found -Dlauncher.stacktrace=true");
            }
            LogHelper.info("Debug mode disabled (found env PRODUCTION)");
        } else {
            LogHelper.info("If need debug output use -Dlauncher.debug=true");
            LogHelper.info("If need stacktrace output use -Dlauncher.stacktrace=true");
            if (LogHelper.isDebugEnabled()) waitProcess = true;
        }
        LogHelper.info("Restart Launcher with JavaAgent...");
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (waitProcess) processBuilder.inheritIO();

        JavaVersion javaVersion = null;
        try {
            if (!noJavaCheck) javaVersion = findJava();
        } catch (Throwable e) {
            LogHelper.error(e);
        }
        if (javaVersion == null) {
            javaVersion = JavaVersion.getCurrentJavaVersion();
        }

        Path javaBin = IOHelper.resolveJavaBin(javaVersion.jvmDir);
        List<String> args = new LinkedList<>();
        args.add(javaBin.toString());
        String pathLauncher = IOHelper.getCodeSource(LauncherEngine.class).toString();
        args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        args.add(JVMHelper.jvmProperty(LogHelper.STACKTRACE_PROPERTY, Boolean.toString(LogHelper.isStacktraceEnabled())));
        args.add(JVMHelper.jvmProperty(LogHelper.DEV_PROPERTY, Boolean.toString(LogHelper.isDevEnabled())));
        JVMHelper.addSystemPropertyToArgs(args, DirBridge.CUSTOMDIR_PROPERTY);
        JVMHelper.addSystemPropertyToArgs(args, DirBridge.USE_CUSTOMDIR_PROPERTY);
        JVMHelper.addSystemPropertyToArgs(args, DirBridge.USE_OPTDIR_PROPERTY);
        JVMHelper.addSystemPropertyToArgs(args, DirWatcher.IGN_OVERFLOW);
        if (javaVersion.version >= 9) {
            LogHelper.debug("Found Java 9+ ( %s )", System.getProperty("java.version"));
            String pathToFx = System.getenv("PATH_TO_FX");
            Path fxPath = pathToFx == null ? null : Paths.get(pathToFx);
            StringBuilder builder = new StringBuilder();
            Path[] findPath = new Path[]{javaVersion.jvmDir, javaVersion.jvmDir.resolve("jre"), fxPath};
            tryAddModule(findPath, "javafx.base", builder);
            tryAddModule(findPath, "javafx.graphics", builder);
            tryAddModule(findPath, "javafx.fxml", builder);
            tryAddModule(findPath, "javafx.controls", builder);
            boolean useSwing = tryAddModule(findPath, "javafx.swing", builder);
            String modulePath = builder.toString();
            if (!modulePath.isEmpty()) {
                args.add("--add-modules");
                String javaModules = "javafx.base,javafx.fxml,javafx.controls,jdk.unsupported";
                if (useSwing) javaModules = javaModules.concat(",javafx.swing");
                args.add(javaModules);
                args.add("--module-path");
                args.add(modulePath);
            }
        }
        args.add(MAGIC_ARG);
        args.add("-XX:+DisableAttachMechanism");
        args.add("-Xmx256M");
        //Collections.addAll(args, "-javaagent:".concat(pathLauncher));
        args.add("-cp");
        args.add(pathLauncher);
        args.add(LauncherEngine.class.getName());
        LauncherEngine.modulesManager.callWrapper(processBuilder, args);
        EnvHelper.addEnv(processBuilder);
        LogHelper.debug("Commandline: " + args);
        processBuilder.command(args);
        Process process = processBuilder.start();
        if (!waitProcess) {
            Thread.sleep(3000);
            if (!process.isAlive()) {
                int errorcode = process.exitValue();
                if (errorcode != 0)
                    LogHelper.error("Process exit with error code: %d", errorcode);
                else
                    LogHelper.info("Process exit with code 0");
            } else {
                LogHelper.debug("Process started success");
            }
        } else {
            process.waitFor();
        }
    }

    public static Path tryFindModule(Path path, String moduleName) {
        Path result = path.resolve(moduleName.concat(".jar"));
        LogHelper.dev("Try resolve %s", result.toString());
        if (!IOHelper.isFile(result))
            result = path.resolve("lib").resolve(moduleName.concat(".jar"));
        else return result;
        if (!IOHelper.isFile(result))
            return null;
        else return result;
    }

    public static boolean tryAddModule(Path[] paths, String moduleName, StringBuilder args) {
        for (Path path : paths) {
            if (path == null) continue;
            Path result = tryFindModule(path, moduleName);
            if (result != null) {
                if (args.length() != 0) args.append(File.pathSeparatorChar);
                args.append(result.toAbsolutePath().toString());
                return true;
            }
        }
        return false;
    }

    public static JavaVersion findJavaByProgramFiles(Path path) {
        LogHelper.debug("Check Java in %s", path.toString());
        JavaVersion selectedJava = null;
        File[] candidates = path.toFile().listFiles(File::isDirectory);
        if (candidates == null) return null;
        for (File candidate : candidates) {
            Path javaPath = candidate.toPath();
            try {
                JavaVersion javaVersion = JavaVersion.getByPath(javaPath);
                if (javaVersion == null || javaVersion.version < 8) continue;
                LogHelper.debug("Found Java %d in %s (javafx %s)", javaVersion.version, javaVersion.jvmDir.toString(), javaVersion.enabledJavaFX ? "true" : "false");
                if (javaVersion.enabledJavaFX && (selectedJava == null || !selectedJava.enabledJavaFX)) {
                    selectedJava = javaVersion;
                    continue;
                }
                if (selectedJava != null && javaVersion.enabledJavaFX && javaVersion.version < selectedJava.version) {
                    selectedJava = javaVersion;
                }
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        if (selectedJava != null) {
            LogHelper.debug("Selected Java %d in %s (javafx %s)", selectedJava.version, selectedJava.jvmDir.toString(), selectedJava.enabledJavaFX ? "true" : "false");
        }
        return selectedJava;
    }

    public static JavaVersion findJava() {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            JavaVersion result = null;
            Path defaultJvmContainerDir = Paths.get(System.getProperty("java.home")).getParent();
            if (defaultJvmContainerDir.getParent().getFileName().toString().contains("x86")) //Program Files (x86) ?
            {
                Path programFiles64 = defaultJvmContainerDir.getParent().getParent().resolve("Program Files").resolve("Java");
                if (IOHelper.isDir(programFiles64)) {
                    result = findJavaByProgramFiles(programFiles64);
                }
            }
            if (result == null) {
                result = findJavaByProgramFiles(defaultJvmContainerDir);
            }
            return result;
        }
        return null;
    }

    public static int getJavaVersion(String version) {
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }
}
