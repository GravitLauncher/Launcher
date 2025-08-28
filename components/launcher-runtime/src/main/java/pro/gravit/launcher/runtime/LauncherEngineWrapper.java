package pro.gravit.launcher.runtime;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.launch.LaunchOptions;
import pro.gravit.utils.launch.ModuleLaunch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@LauncherNetworkAPI
public class LauncherEngineWrapper {
    private static final List<String> modules = new ArrayList<>();

    static {
        modules.add("javafx.base");
        modules.add("javafx.graphics");
        modules.add("javafx.fxml");
        modules.add("javafx.controls");
        modules.add("javafx.media");
        modules.add("javafx.web");
    }
    public static void main(String[] args) throws Throwable {
        ModuleLaunch launch = new ModuleLaunch();
        LaunchOptions options = new LaunchOptions();
        options.disablePackageDelegateSupport = true;
        options.moduleConf = new LaunchOptions.ModuleConf();
        List<Path> classpath = new ArrayList<>();
        classpath.add(IOHelper.getCodeSource(LauncherEngine.class));
        var libDirectory = Path.of(System.getProperty("java.home")).resolve("lib");
        for(var moduleName : modules) {
            var path = libDirectory.resolve(moduleName.concat(".jar"));
            if(Files.exists(path)) {
                options.moduleConf.modules.add(moduleName);
                options.moduleConf.modulePath.add(path.toAbsolutePath().toString());
            }
        }
        var control = launch.init(classpath, null, options);
        launch.launch(LauncherEngine.class.getName(), null, List.of(args));
    }
}
