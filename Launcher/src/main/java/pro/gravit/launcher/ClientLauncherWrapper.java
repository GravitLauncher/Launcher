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
    public static final String NO_JAVA9_CHECK_PROPERTY = "launcher.noJava9Check";
    public static boolean noJava9check = Boolean.getBoolean(NO_JAVA9_CHECK_PROPERTY);
    public static boolean waitProcess = Boolean.getBoolean(WAIT_PROCESS_PROPERTY);

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
        Path currentJavaDirPath = Paths.get(System.getProperty("java.home"));
        try {
            Path nextJavaDirPath = findCorrectJava(currentJavaDirPath.getParent());
            if(nextJavaDirPath != null)
            {
                currentJavaDirPath = nextJavaDirPath;
            }
        } catch (Throwable e)
        {
            LogHelper.error(e);
        }
        Path javaBin = IOHelper.resolveJavaBin(currentJavaDirPath);
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
        if (!noJava9check && !System.getProperty("java.version").startsWith("1.8")) {
            LogHelper.debug("Found Java 9+ ( %s )", System.getProperty("java.version"));
            String pathToFx = System.getenv("PATH_TO_FX");
            Path fxPath = pathToFx == null ? null : Paths.get(pathToFx);
            StringBuilder builder = new StringBuilder();
            Path[] findPath = new Path[]{currentJavaDirPath, fxPath};
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

    public static Path findCorrectJava(Path javaContainerDir)
    {
        if(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE)
        {
            LogHelper.debug("Java container path: %s", javaContainerDir.toString());
            File programFiles = javaContainerDir.toFile();
            File[] candidates = programFiles.listFiles(File::isDirectory);
            if(candidates == null) return null;
            int resultJavaVersion = 0;
            Path resultJavaPath = null;
            for(File candidate : candidates)
            {
                //Try get version
                LogHelper.debug("Java candidate: %s", candidate.toPath().toString());
                try {
                    Path releaseFile = candidate.toPath().resolve("release");
                    if(!IOHelper.isFile(releaseFile)) continue;
                    Properties properties = new Properties();
                    properties.load(IOHelper.newReader(releaseFile));
                    int javaVersion = getJavaVersion(properties.getProperty("JAVA_VERSION"));
                    if(javaVersion >= 8 && (resultJavaVersion == 0 || javaVersion < resultJavaVersion))
                    {
                        if(javaVersion > 8)
                        {
                            //Try check correct javafx
                            Path baseJavaFx = tryFindModule(candidate.toPath(), "javafx.base");
                            if(baseJavaFx == null) continue;
                        }
                        resultJavaVersion = javaVersion;
                        resultJavaPath = candidate.toPath();
                    }
                } catch (Throwable e)
                {
                    LogHelper.debug("Java candidate %s throws exception %s", candidate.toPath(), e.getClass().getName());
                }
            }
            if(resultJavaVersion < 9) noJava9check = true;
            return resultJavaPath;
        }
        return null;
    }

    public static int getJavaVersion(String version)
    {
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
