package pro.gravit.launchserver;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.launch.ClassLoaderControl;
import pro.gravit.utils.launch.LaunchOptions;
import pro.gravit.utils.launch.ModuleLaunch;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Main {
    private static final List<String> classpathOnly = List.of("proguard", "jline", "kotlin", "epoll");
    private static boolean isClasspathOnly(Path path) {
        var fileName = path.getFileName().toString();
        for(var e : classpathOnly) {
            if(fileName.contains(e)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Throwable {
        ModuleLaunch launch = new ModuleLaunch();
        LaunchOptions options = new LaunchOptions();
        options.disablePackageDelegateSupport = true;
        options.moduleConf = new LaunchOptions.ModuleConf();
        List<Path> libraries;
        try(Stream<Path> files = Files.walk(Path.of("libraries"), FileVisitOption.FOLLOW_LINKS)) {
            libraries = new ArrayList<>(files.filter(e -> e.getFileName().toString().endsWith(".jar")).toList());
        }
        List<Path> classpath = new ArrayList<>();
        List<String> modulepath = new ArrayList<>();
        for(var l : libraries) {
            if(isClasspathOnly(l)) {
                classpath.add(l);
            } else {
                modulepath.add(l.toAbsolutePath().toString());
            }
        }
        classpath.add(IOHelper.getCodeSource(LaunchServerStarter.class));
        options.moduleConf.modulePath.addAll(modulepath);
        options.moduleConf.modules.add("ALL-MODULE-PATH");
        ClassLoaderControl control = launch.init(classpath, "natives", options);
        ModuleLayer.Controller controller = (ModuleLayer.Controller) control.getJava9ModuleController();
        launch.launch("pro.gravit.launchserver.LaunchServerStarter", null, Arrays.asList(args));
    }
}
