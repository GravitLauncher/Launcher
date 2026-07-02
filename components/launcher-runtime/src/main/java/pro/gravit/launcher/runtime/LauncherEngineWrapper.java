package pro.gravit.launcher.runtime;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.core.LauncherInject;
import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.start.ClientLauncherWrapper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.launch.ClassLoaderControl;
import pro.gravit.utils.launch.LaunchOptions;
import pro.gravit.utils.launch.ModuleLaunch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LauncherNetworkAPI
public class LauncherEngineWrapper {
    private static final List<String> modules = new ArrayList<>();
    @LauncherInject("launcher.alwaysUseRelauncher")
    public static boolean alwaysUseRelauncher;



    public static Map<String, String> applyLoggerSettings(String[] arguments) {
        Map<String, String> map = new HashMap<>();
        LauncherConfig config = Launcher.getConfig();
        {
            List<String> argsList = List.of(arguments);
            boolean isEnabledDebug = false;
            if(argsList.contains("--dev") && !config.environment.equals(LauncherConfig.LauncherEnvironment.PROD)) {
                map.put("org.slf4j.simpleLogger.defaultLogLevel", "trace");
                isEnabledDebug = true;
            } else if(argsList.contains("--debug")) {
                map.put("org.slf4j.simpleLogger.defaultLogLevel", "debug");
                isEnabledDebug = true;
            } else {
                map.put("org.slf4j.simpleLogger.defaultLogLevel", "info");
            }
            if(argsList.contains("--output")) {
                map.put("org.slf4j.simpleLogger.logFile", "Launcher.log");
            } else {
                map.put("org.slf4j.simpleLogger.logFile", "System.out");
            }
            if(!config.environment.equals(LauncherConfig.LauncherEnvironment.DEV)) {
                map.put("org.slf4j.simpleLogger.showLogName", "false");
                map.put("org.slf4j.simpleLogger.showThreadName", "false");
            }
            map.put("org.slf4j.simpleLogger.showDateTime", "true");
            map.put("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss:SSS");
        }
        map.put("launcher.useSlf4j", "true");
        for(var e : map.entrySet()) {
            System.setProperty(e.getKey(), e.getValue());
        }
        return map;
    }

    static {
        modules.add("javafx.base");
        modules.add("javafx.graphics");
        modules.add("javafx.fxml");
        modules.add("javafx.controls");
        modules.add("javafx.media");
        modules.add("javafx.web");
        modules.add("jdk.jsobject"); // https://bugs.openjdk.org/browse/JDK-8338249
    }
    public static void main(String[] args) throws Throwable {
        if (alwaysUseRelauncher && !Boolean.getBoolean(ClientLauncherWrapper.WRAPPED_LAUNCH_PROPERTY)) {
            System.setProperty(ClientLauncherWrapper.BRIDGED_FROM_ENGINE_PROPERTY, "true");
            ClientLauncherWrapper.main(args);
            return;
        }
        applyLoggerSettings(args);
        ModuleLaunch launch = new ModuleLaunch();
        LaunchOptions options = new LaunchOptions();
        options.disablePackageDelegateSupport = true;
        options.moduleConf = new LaunchOptions.ModuleConf();
        List<Path> classpath = new ArrayList<>();
        classpath.add(IOHelper.getCodeSource(LauncherEngine.class));
        var libDirectory = Path.of(System.getProperty("java.home")).resolve("lib");
        int failedModules = 0;
        for(var moduleName : modules) {
            var path = libDirectory.resolve(moduleName.concat(".jar"));
            if(Files.exists(path)) {
                options.moduleConf.modules.add(moduleName);
                options.moduleConf.modulePath.add(path.toAbsolutePath().toString());
            } else {
                failedModules++;
            }
        }
        ClassLoaderControl control;
        try {
            control = launch.init(classpath, null, options);
            ModuleLayer.Controller controller = (ModuleLayer.Controller) control.getJava9ModuleController();
            var javafxModule = controller.layer().findModule("javafx.base");
            if(javafxModule.isEmpty()) {
                // We probably failed to resolve javafx modules, try re-launch
                if(!Boolean.getBoolean(ClientLauncherWrapper.WRAPPED_LAUNCH_PROPERTY)) {
                    System.setProperty(ClientLauncherWrapper.BRIDGED_FROM_ENGINE_PROPERTY, "true");
                    ClientLauncherWrapper.main(args);
                    return;
                }
            }
        } catch (Throwable e) {
            // We probably failed to resolve javafx modules, try re-launch
            if(!Boolean.getBoolean(ClientLauncherWrapper.WRAPPED_LAUNCH_PROPERTY)) {
                System.setProperty(ClientLauncherWrapper.BRIDGED_FROM_ENGINE_PROPERTY, "true");
                ClientLauncherWrapper.main(args);
                return;
            }
            throw e;
        }
        launch.launch(LauncherEngine.class.getName(), null, List.of(args));
    }
}
