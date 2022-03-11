package pro.gravit.launcher;

import pro.gravit.launcher.client.ClientModuleManager;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.utils.DirWatcher;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientLauncherWrapper {
    public static final String MAGIC_ARG = "-Djdk.attach.allowAttachSelf";
    public static final String WAIT_PROCESS_PROPERTY = "launcher.waitProcess";
    public static final String NO_JAVA_CHECK_PROPERTY = "launcher.noJavaCheck";
    public static boolean noJavaCheck = Boolean.getBoolean(NO_JAVA_CHECK_PROPERTY);
    public static boolean waitProcess = Boolean.getBoolean(WAIT_PROCESS_PROPERTY);
    @LauncherInject("launcher.memory")
    public static int launcherMemoryLimit;
    @LauncherInject("launcher.customJvmOptions")
    public static List<String> customJvmOptions;

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
                List<JavaHelper.JavaVersion> javaVersions = JavaHelper.findJava();
                for (JavaHelper.JavaVersion version : javaVersions) {
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
            context.javaVersion = JavaHelper.JavaVersion.getCurrentJavaVersion();
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
            Path openjfxPath = JavaHelper.tryGetOpenJFXPath(context.javaVersion.jvmDir);
            if (openjfxPath != null) {
                context.javaFXPaths.add(openjfxPath);
            }
            StringBuilder modulesPath = new StringBuilder();
            StringBuilder modulesAdd = new StringBuilder();
            for (String moduleName : context.jvmModules) {
                boolean success = JavaHelper.tryAddModule(context.javaFXPaths, moduleName, modulesPath);
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
        if(customJvmOptions != null) {
            args.addAll(customJvmOptions);
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


    public static class ClientLauncherWrapperContext {
        public JavaHelper.JavaVersion javaVersion;
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
