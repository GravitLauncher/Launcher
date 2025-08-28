package pro.gravit.launcher.start;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.core.LauncherInject;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.*;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
    public static RuntimeModuleManager modulesManager;

    public static boolean contains(String[] array, String value) {
        for(String s : array) {
            if(s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] arguments) throws IOException, InterruptedException {
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        JVMHelper.checkStackTrace(ClientLauncherWrapper.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParams();
        LauncherConfig config = Launcher.getConfig();
        modulesManager = new RuntimeModuleManager();
        LauncherConfig.initModules(modulesManager);
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
            if(contains(arguments, "--debug")) {
                LogHelper.setDebugEnabled(true);
                LogHelper.setStacktraceEnabled(true);
            }
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

        if(context.javaVersion.version < 17) {
            String message = String.format("GravitLauncher v%s required Java 17 or higher", Version.getVersion());
            LogHelper.error(message);
            JOptionPane.showMessageDialog(null, message, "GravitLauncher", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        context.executePath = IOHelper.resolveJavaBin(context.javaVersion.jvmDir);
        String pathLauncher = IOHelper.getCodeSource(ClientLauncherWrapper.class).toString();
        context.mainClass = "pro.gravit.launcher.runtime.LauncherEngineWrapper";
        context.memoryLimit = launcherMemoryLimit;
        context.classpath.add(pathLauncher);
        context.jvmProperties.put(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled()));
        context.jvmProperties.put(LogHelper.STACKTRACE_PROPERTY, Boolean.toString(LogHelper.isStacktraceEnabled()));
        context.jvmProperties.put(LogHelper.DEV_PROPERTY, Boolean.toString(LogHelper.isDevEnabled()));
        context.jvmModules.add("javafx.base");
        context.jvmModules.add("javafx.graphics");
        context.jvmModules.add("javafx.fxml");
        context.jvmModules.add("javafx.controls");
        context.jvmModules.add("javafx.media");
        context.jvmModules.add("javafx.web");
        context.args.add(MAGIC_ARG);
        context.args.add("-XX:+DisableAttachMechanism");
        context.clientArgs.addAll(Arrays.asList(arguments));
        EnvHelper.addEnv(context.processBuilder);
        modulesManager.callWrapper(context);
        // ---------
        List<String> args = new ArrayList<>(16);
        args.add(context.executePath.toAbsolutePath().toString());
        args.addAll(context.args);
        context.jvmProperties.forEach((key, value) -> args.add(String.format("-D%s=%s", key, value)));
        if (context.memoryLimit != 0) {
            args.add(String.format("-Xmx%dM", context.memoryLimit));
        }
        if (customJvmOptions != null) {
            args.addAll(customJvmOptions);
        }
        if(context.useLegacyClasspathProperty) {
            args.add(String.format("-Djava.class.path=%s", String.join(IOHelper.PLATFORM_SEPARATOR, context.classpath)));
        } else {
            args.add("-cp");
            args.add(String.join(IOHelper.PLATFORM_SEPARATOR, context.classpath));
        }
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
        public boolean useLegacyClasspathProperty;
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
