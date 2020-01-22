package pro.gravit.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import pro.gravit.launcher.client.ClientLauncher;
import pro.gravit.launcher.client.ClientModuleManager;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.utils.helper.EnvHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

public class ClientLauncherWrapper {
    public static final String MAGIC_ARG = "-Djdk.attach.allowAttachSelf";
    public static final String WAIT_PROCESS_PROPERTY = "launcher.waitProcess";
    public static final String NO_JAVA9_CHECK_PROPERTY = "launcher.noJava9Check";
    public static boolean waitProcess = Boolean.getBoolean(WAIT_PROCESS_PROPERTY);
    public static boolean noJava9check = Boolean.getBoolean(NO_JAVA9_CHECK_PROPERTY);

    public static void main(String[] arguments) throws IOException, InterruptedException {
        LogHelper.printVersion("Launcher");
        LogHelper.printLicense("Launcher");
        JVMHelper.checkStackTrace(ClientLauncherWrapper.class);
        JVMHelper.verifySystemProperties(Launcher.class, true);
        EnvHelper.checkDangerousParams();
        LauncherConfig config = Launcher.getConfig();
        LauncherEngine.modulesManager = new ClientModuleManager();
        LauncherConfig.getAutogenConfig().initModules();

        LogHelper.info("Launcher for project %s", config.projectname);
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
        Path javaBin = IOHelper.resolveJavaBin(Paths.get(System.getProperty("java.home")));
        List<String> args = new LinkedList<>();
        args.add(javaBin.toString());
        String pathLauncher = IOHelper.getCodeSource(ClientLauncher.class).toString();
        args.add(JVMHelper.jvmProperty(LogHelper.DEBUG_PROPERTY, Boolean.toString(LogHelper.isDebugEnabled())));
        args.add(JVMHelper.jvmProperty(LogHelper.STACKTRACE_PROPERTY, Boolean.toString(LogHelper.isStacktraceEnabled())));
        args.add(JVMHelper.jvmProperty(LogHelper.DEV_PROPERTY, Boolean.toString(LogHelper.isDevEnabled())));
        JVMHelper.addSystemPropertyToArgs(args, DirBridge.CUSTOMDIR_PROPERTY);
        JVMHelper.addSystemPropertyToArgs(args, DirBridge.USE_CUSTOMDIR_PROPERTY);
        JVMHelper.addSystemPropertyToArgs(args, DirBridge.USE_OPTDIR_PROPERTY);
        if (!noJava9check && !System.getProperty("java.version").startsWith("1.8"))
        {
            LogHelper.debug("Found Java 9+ ( %s )", System.getProperty("java.version"));
            Path jvmDir = Paths.get(System.getProperty("java.home"));
            String pathToFx = System.getenv("PATH_TO_FX");
            Path fxPath = pathToFx == null ? null : Paths.get(pathToFx);
            StringBuilder builder = new StringBuilder();
            Path[] findPath = new Path[]{jvmDir, fxPath};
            tryAddModule(findPath, "javafx.base", builder);
            tryAddModule(findPath, "javafx.graphics", builder);
            tryAddModule(findPath, "javafx.fxml", builder);
            tryAddModule(findPath, "javafx.controls", builder);
            String modulePath = builder.toString();
            if(!modulePath.isEmpty())
            {
                Collections.addAll(args, "--module-path");
                Collections.addAll(args, modulePath);
                Collections.addAll(args, "--add-modules");
                Collections.addAll(args, "javafx.base,javafx.fxml,javafx.controls,jdk.unsupported");
            }
        }
        Collections.addAll(args, MAGIC_ARG);
        Collections.addAll(args, "-XX:+DisableAttachMechanism");
        Collections.addAll(args, "-javaagent:".concat(pathLauncher));
        Collections.addAll(args, "-cp");
        Collections.addAll(args, pathLauncher);
        Collections.addAll(args, LauncherEngine.class.getName());
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
    public static Path tryFindModule(Path path, String moduleName)
    {
        Path result = path.resolve(moduleName.concat(".jar"));
        LogHelper.dev("Try resolve %s", result.toString());
        if(!IOHelper.isFile(result))
            result = path.resolve("lib").resolve(moduleName.concat(".jar"));
        else return result;
        if(!IOHelper.isFile(result))
            return null;
        else return result;
    }
    public static boolean tryAddModule(Path[] paths, String moduleName, StringBuilder args)
    {
        for(Path path : paths)
        {
            if(path == null) continue;
            Path result = tryFindModule(path, moduleName);
            if(result != null)
            {
                if(args.length() != 0) args.append(File.pathSeparatorChar);
                args.append(result.toAbsolutePath().toString());
                return true;
            }
        }
        return false;
    }
}
