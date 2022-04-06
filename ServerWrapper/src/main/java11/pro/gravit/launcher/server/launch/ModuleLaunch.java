package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.IOHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleLaunch implements Launch {
    @Override
    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void run(ServerWrapper.Config config, String[] args) throws Throwable {
        URL[] urls = config.classpath.stream().map(Paths::get).map(IOHelper::toURL).toArray(URL[]::new);
        ClassLoader ucl = new PublicURLClassLoader(urls);
        // Create Module Layer
        ModuleFinder finder = ModuleFinder.of(config.moduleConf.modulePath.stream().map(Paths::get).toArray(Path[]::new));
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration configuration = bootLayer.configuration()
                .resolveAndBind(ModuleFinder.of(), finder, config.moduleConf.modules);
        ModuleLayer.Controller controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(bootLayer), ucl);
        ModuleLayer layer = controller.layer();
        // Configure exports / opens
        for(var e : config.moduleConf.exports.entrySet()) {
            String[] split = e.getKey().split("\\\\");
            Module source = layer.findModule(split[0]).orElseThrow();
            String pkg = split[1];
            Module target = layer.findModule(e.getValue()).orElseThrow();
            controller.addExports(source, pkg, target);
        }
        for(var e : config.moduleConf.opens.entrySet()) {
            String[] split = e.getKey().split("\\\\");
            Module source = layer.findModule(split[0]).orElseThrow();
            String pkg = split[1];
            Module target = layer.findModule(e.getValue()).orElseThrow();
            controller.addOpens(source, pkg, target);
        }
        for(var e : config.moduleConf.reads.entrySet()) {
            Module source = layer.findModule(e.getKey()).orElseThrow();
            Module target = layer.findModule(e.getValue()).orElseThrow();
            controller.addReads(source, target);
        }
        Module mainModule = layer.findModule(config.moduleConf.mainModule).orElseThrow();
        Module unnamed = ModuleLaunch.class.getClassLoader().getUnnamedModule();
        if(unnamed != null) {
            controller.addOpens(mainModule, getPackageFromClass(config.mainclass), unnamed);
        }
        // Start main class
        ClassLoader loader = mainModule.getClassLoader();
        Class<?> mainClass = Class.forName(config.mainclass, true, loader);
        MethodHandle mainMethod = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
        mainMethod.invoke(args);
    }

    private static String getPackageFromClass(String clazz) {
        int index = clazz.lastIndexOf(".");
        if(index >= 0) {
            return clazz.substring(0, index);
        }
        return clazz;
    }
}
