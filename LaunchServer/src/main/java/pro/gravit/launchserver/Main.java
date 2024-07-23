package pro.gravit.launchserver;

import pro.gravit.launchserver.holder.LaunchServerControlHolder;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.launch.ClassLoaderControl;
import pro.gravit.utils.launch.LaunchOptions;
import pro.gravit.utils.launch.ModuleLaunch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Main {
    private static final List<String> classpathOnly = List.of("proguard", "jline", "progressbar", "kotlin", "epoll");
    private static final String LOG4J_PROPERTY = "log4j2.configurationFile";
    private static final String DEBUG_PROPERTY = "launchserver.main.debug";
    private static final String LIBRARIES_PROPERTY = "launchserver.dir.libraries";
    private static boolean isClasspathOnly(Path path) {
        var fileName = path.getFileName().toString();
        for(var e : classpathOnly) {
            if(fileName.contains(e)) {
                return true;
            }
        }
        return false;
    }

    private static void unpackLog4j() {
        String log4jConfigurationFile = System.getProperty(LOG4J_PROPERTY);
        if(log4jConfigurationFile == null) {
            Path log4jConfigPath = Path.of("log4j2.xml");
            if(!Files.exists(log4jConfigPath)) {
                try(FileOutputStream output = new FileOutputStream(log4jConfigPath.toFile())) {
                    try(InputStream input = Main.class.getResourceAsStream("/log4j2.xml")) {
                        if(input == null) {
                            return;
                        }
                        input.transferTo(output);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            System.setProperty(LOG4J_PROPERTY, log4jConfigPath.toAbsolutePath().toString());
        }
    }

    public static void main(String[] args) throws Throwable {
        unpackLog4j();
        ModuleLaunch launch = new ModuleLaunch();
        LaunchOptions options = new LaunchOptions();
        options.moduleConf = new LaunchOptions.ModuleConf();
        Path librariesPath = Path.of(System.getProperty(LIBRARIES_PROPERTY, "libraries"));
        List<Path> libraries;
        try(Stream<Path> files = Files.walk(librariesPath, FileVisitOption.FOLLOW_LINKS)) {
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
        control.clearLauncherPackages();
        control.addLauncherPackage("pro.gravit.utils.launch");
        control.addLauncherPackage("pro.gravit.launchserver.holder");
        ModuleLayer.Controller controller = (ModuleLayer.Controller) control.getJava9ModuleController();
        LaunchServerControlHolder.setControl(control);
        LaunchServerControlHolder.setController(controller);
        if(Boolean.getBoolean(DEBUG_PROPERTY)) {
            for(var e : controller.layer().modules()) {
                System.out.printf("Module %s\n", e.getName());
                for(var p : e.getPackages()) {
                    System.out.printf("Package %s\n", p);
                }
            }
        }
        launch.launch("pro.gravit.launchserver.LaunchServerStarter", null, Arrays.asList(args));
    }
}
