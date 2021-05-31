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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClientLauncherWrapper {
    public static final String MAGIC_ARG = "-Djdk.attach.allowAttachSelf";
    public static final String WAIT_PROCESS_PROPERTY = "launcher.waitProcess";
    public static final String NO_JAVA_CHECK_PROPERTY = "launcher.noJavaCheck";
    public static boolean noJavaCheck = Boolean.getBoolean(NO_JAVA_CHECK_PROPERTY);
    public static boolean waitProcess = Boolean.getBoolean(WAIT_PROCESS_PROPERTY);
    @LauncherInject("launcher.memory")
    public static int launcherMemoryLimit;

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
        ClientLauncherWrapperContext context = new ClientLauncherWrapperContext();
        context.processBuilder = new ProcessBuilder();
        if (waitProcess) context.processBuilder.inheritIO();

        context.javaVersion = null;
        try {
            if (!noJavaCheck) {
                List<JavaVersion> javaVersions = findJava();
                for (JavaVersion version : javaVersions) {
                    LogHelper.debug("Found Java %d b%d in %s javafx %s", version.version, version.build, version.jvmDir.toString(), version.enabledJavaFX ? "supported" : "not supported");
                    if (context.javaVersion == null) {
                        context.javaVersion = version;
                        continue;
                    }
                    if (version.enabledJavaFX && !context.javaVersion.enabledJavaFX) {
                        context.javaVersion = version;
                        continue;
                    }
                    if (version.enabledJavaFX == context.javaVersion.enabledJavaFX) {
                        if (context.javaVersion.version < version.version) {
                            context.javaVersion = version;
                        } else if (context.javaVersion.version == version.version && context.javaVersion.build < version.build) {
                            context.javaVersion = version;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LogHelper.error(e);
        }
        if (context.javaVersion == null) {
            context.javaVersion = JavaVersion.getCurrentJavaVersion();
        }

        context.executePath = IOHelper.resolveJavaBin(context.javaVersion.jvmDir);
        //List<String> args = new LinkedList<>();
        //args.add(javaBin.toString());
        String pathLauncher = IOHelper.getCodeSource(LauncherEngine.class).toString();
        context.mainClass = LauncherEngine.class.getName();
        context.memoryLimit = launcherMemoryLimit;
        context.classpath.add(pathLauncher);
        context.jvmProperties.put(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled()));
        context.jvmProperties.put(LogHelper.STACKTRACE_PROPERTY, Boolean.toString(LogHelper.isStacktraceEnabled()));
        context.jvmProperties.put(LogHelper.DEV_PROPERTY, Boolean.toString(LogHelper.isDevEnabled()));
        context.addSystemProperty(DirBridge.CUSTOMDIR_PROPERTY);
        context.addSystemProperty(DirBridge.USE_CUSTOMDIR_PROPERTY);
        context.addSystemProperty(DirBridge.USE_OPTDIR_PROPERTY);
        context.addSystemProperty(DirWatcher.IGN_OVERFLOW);
        context.jvmModules.add("javafx.base");
        context.jvmModules.add("javafx.graphics");
        context.jvmModules.add("javafx.fxml");
        context.jvmModules.add("javafx.controls");
        context.jvmModules.add("javafx.swing");
        context.jvmModules.add("javafx.media");
        context.jvmModules.add("javafx.web");
        context.args.add(MAGIC_ARG);
        context.args.add("-XX:+DisableAttachMechanism");
        EnvHelper.addEnv(context.processBuilder);
        LauncherEngine.modulesManager.callWrapper(context);
        // ---------
        List<String> args = new ArrayList<>(16);
        args.add(context.executePath.toAbsolutePath().toString());
        args.addAll(context.args);
        context.jvmProperties.forEach((key, value) -> args.add(String.format("-D%s=%s", key, value)));
        if (context.javaVersion.version >= 9) {
            context.javaFXPaths.add(context.javaVersion.jvmDir);
            context.javaFXPaths.add(context.javaVersion.jvmDir.resolve("jre"));
            Path openjfxPath = tryGetOpenJFXPath(context.javaVersion.jvmDir);
            if (openjfxPath != null) {
                context.javaFXPaths.add(openjfxPath);
            }
            StringBuilder modulesPath = new StringBuilder();
            StringBuilder modulesAdd = new StringBuilder();
            for (String moduleName : context.jvmModules) {
                boolean success = tryAddModule(context.javaFXPaths, moduleName, modulesPath);
                if (success) {
                    if (modulesAdd.length() > 0) modulesAdd.append(",");
                    modulesAdd.append(moduleName);
                }
            }
            if (modulesAdd.length() > 0) {
                args.add("--add-modules");
                args.add(modulesAdd.toString());
            }
            if (modulesPath.length() > 0) {
                args.add("--module-path");
                args.add(modulesPath.toString());
            }
        }
        if (context.memoryLimit != 0) {
            args.add(String.format("-Xmx%dM", context.memoryLimit));
        }
        args.add("-cp");
        args.add(String.join(IOHelper.PLATFORM_SEPARATOR, context.classpath));
        args.add(context.mainClass);
        args.addAll(context.clientArgs);
        LogHelper.debug("Commandline: " + args);
        context.processBuilder.command(args);
        Process process = context.processBuilder.start();
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

    public static Path tryGetOpenJFXPath(Path jvmDir) {
        String dirName = jvmDir.getFileName().toString();
        Path parent = jvmDir.getParent();
        if (parent == null) return null;
        Path archJFXPath = parent.resolve(dirName.replace("openjdk", "openjfx"));
        if (Files.isDirectory(archJFXPath)) {
            return archJFXPath;
        }
        Path arch2JFXPath = parent.resolve(dirName.replace("jdk", "openjfx"));
        if (Files.isDirectory(arch2JFXPath)) {
            return arch2JFXPath;
        }
        if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            Path debianJfxPath = Paths.get("/usr/share/openjfx");
            if (Files.isDirectory(debianJfxPath)) {
                return debianJfxPath;
            }
        }
        return null;
    }

    public static Path tryFindModule(Path path, String moduleName) {
        Path result = path.resolve(moduleName.concat(".jar"));
        if (!IOHelper.isFile(result))
            result = path.resolve("lib").resolve(moduleName.concat(".jar"));
        else return result;
        if (!IOHelper.isFile(result))
            return null;
        else return result;
    }

    public static boolean tryAddModule(List<Path> paths, String moduleName, StringBuilder args) {
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

    public static List<JavaVersion> findJava() {
        List<String> javaPaths = new ArrayList<>(4);
        List<JavaVersion> result = new ArrayList<>(4);
        try {
            tryAddJava(javaPaths, result, ClientLauncherWrapper.JavaVersion.getCurrentJavaVersion());
        } catch (IOException e) {
            LogHelper.error(e);
        }
        String[] path = System.getenv("PATH").split(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? ";" : ":");
        for (String p : path) {
            try {
                Path p1 = Paths.get(p);
                Path javaExecPath = JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? p1.resolve("java.exe") : p1.resolve("java");
                if (Files.exists(javaExecPath)) {
                    if (Files.isSymbolicLink(javaExecPath)) {
                        javaExecPath = javaExecPath.toRealPath();
                    }
                    p1 = javaExecPath.getParent().getParent();
                    tryAddJava(javaPaths, result, ClientLauncherWrapper.JavaVersion.getByPath(p1));
                    trySearchJava(javaPaths, result, p1.getParent());
                }
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            Path rootDrive = Paths.get(System.getProperty("java.home"));
            try {
                trySearchJava(javaPaths, result, rootDrive.resolve("Program Files").resolve("Java"));
                trySearchJava(javaPaths, result, rootDrive.resolve("Program Files").resolve("AdoptOpenJDK"));
            } catch (IOException e) {
                LogHelper.error(e);
            }
        } else if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            try {
                trySearchJava(javaPaths, result, Paths.get("/usr/lib/jvm"));
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        return result;
    }

    public static boolean tryAddJava(List<String> javaPaths, List<JavaVersion> result, ClientLauncherWrapper.JavaVersion version) throws IOException {
        if (version == null) return false;
        String path = version.jvmDir.toAbsolutePath().toString();
        if (javaPaths.contains(path)) return false;
        javaPaths.add(path);
        result.add(version);
        return true;
    }

    public static void trySearchJava(List<String> javaPaths, List<JavaVersion> result, Path path) throws IOException {
        if (!Files.isDirectory(path)) return;
        Files.list(path).filter(p -> Files.exists(p.resolve("bin").resolve(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE ? "java.exe" : "java"))).forEach(e -> {
            try {
                tryAddJava(javaPaths, result, JavaVersion.getByPath(e));
                if (Files.exists(e.resolve("jre"))) {
                    tryAddJava(javaPaths, result, JavaVersion.getByPath(e.resolve("jre")));
                }
            } catch (IOException ioException) {
                LogHelper.error(ioException);
            }
        });
    }


    public static class JavaVersionAndBuild {
        public int version;
        public int build;

        public JavaVersionAndBuild(int version, int build) {
            this.version = version;
            this.build = build;
        }

        public JavaVersionAndBuild() {
        }
    }

    public static JavaVersionAndBuild getJavaVersion(String version) {
        JavaVersionAndBuild result = new JavaVersionAndBuild();
        if (version.startsWith("1.")) {
            result.version = Integer.parseInt(version.substring(2, 3));
            result.build = Integer.parseInt(version.substring(version.indexOf('_') + 1));
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                result.version = Integer.parseInt(version.substring(0, dot));
                dot = version.lastIndexOf(".");
                result.build = Integer.parseInt(version.substring(dot + 1));
            }
        }
        return result;
    }

    public static class JavaVersion {
        public final Path jvmDir;
        public final int version;
        public final int build;
        public boolean enabledJavaFX;

        public JavaVersion(Path jvmDir, int version) {
            this.jvmDir = jvmDir;
            this.version = version;
            this.build = 0;
            this.enabledJavaFX = true;
        }

        public JavaVersion(Path jvmDir, int version, int build, boolean enabledJavaFX) {
            this.jvmDir = jvmDir;
            this.version = version;
            this.build = build;
            this.enabledJavaFX = enabledJavaFX;
        }

        public static JavaVersion getCurrentJavaVersion() {
            return new JavaVersion(Paths.get(System.getProperty("java.home")), JVMHelper.getVersion(), 0, isCurrentJavaSupportJavaFX());
        }

        private static boolean isCurrentJavaSupportJavaFX() {
            try {
                Class.forName("javafx.application.Application");
                return true;
            } catch (ClassNotFoundException e) {
                if (JVMHelper.getVersion() > 8) {
                    Path jvmDir = Paths.get(System.getProperty("java.home"));
                    return tryFindModule(jvmDir, "javafx.base") != null;
                }
                return false;
            }
        }

        public static JavaVersion getByPath(Path jvmDir) throws IOException {
            Path releaseFile = jvmDir.resolve("release");
            JavaVersionAndBuild versionAndBuild;
            if (IOHelper.isFile(releaseFile)) {
                Properties properties = new Properties();
                properties.load(IOHelper.newReader(releaseFile));
                versionAndBuild = getJavaVersion(properties.getProperty("JAVA_VERSION").replaceAll("\"", ""));
            } else {
                versionAndBuild = new JavaVersionAndBuild(isExistExtJavaLibrary(jvmDir, "jfxrt") ? 8 : 9, 0);
            }
            JavaVersion resultJavaVersion = new JavaVersion(jvmDir, versionAndBuild.version, versionAndBuild.build, false);
            if (versionAndBuild.version <= 8) {
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

    public static class ClientLauncherWrapperContext {
        public JavaVersion javaVersion;
        public Path executePath;
        public String mainClass;
        public int memoryLimit;
        public ProcessBuilder processBuilder;
        public List<String> args = new ArrayList<>(8);
        public Map<String, String> jvmProperties = new HashMap<>();
        public List<String> classpath = new ArrayList<>();
        public List<String> jvmModules = new ArrayList<>();
        public List<String> clientArgs = new ArrayList<>();
        public List<Path> javaFXPaths = new ArrayList<>();

        public void addSystemProperty(String name) {
            String property = System.getProperty(name);
            if (property != null)
                jvmProperties.put(name, property);
        }
    }
}
