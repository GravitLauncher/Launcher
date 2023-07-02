package pro.gravit.launcher.server.launch;

import pro.gravit.launcher.server.ServerWrapper;
import pro.gravit.utils.PublicURLClassLoader;
import pro.gravit.utils.helper.IOHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ModuleLaunch implements Launch {

    @Override
    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public void run(String mainClass, ServerWrapper.Config config, String[] args) throws Throwable {
        URL[] urls = config.classpath.stream()
                .map(Paths::get)
                .map(IOHelper::toURL)
                .toArray(URL[]::new);


        // Create Module Layer
        ModuleFinder finder = ModuleFinder.of(config.moduleConf.modulePath.stream()
                .map(Paths::get)
                .toArray(Path[]::new)
        );

        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration configuration = bootLayer.configuration().resolveAndBind(ModuleFinder.of(), finder, config.moduleConf.modules);
        ModuleLayer.Controller controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(bootLayer), new PublicURLClassLoader(urls));
        ModuleLayer layer = controller.layer();

        // Configure exports / opens
        for (var e : config.moduleConf.exports.entrySet()) {
            String[] split = e.getKey().split("\\\\");

            controller.addExports(
                    layer.findModule(split[0]).orElseThrow(),
                    split[1],
                    layer.findModule(e.getValue()).orElseThrow()
            );
        }

        for (var e : config.moduleConf.opens.entrySet()) {
            String[] split = e.getKey().split("\\\\");

            controller.addOpens(
                    layer.findModule(split[0]).orElseThrow(),
                    split[1],
                    layer.findModule(e.getValue()).orElseThrow()
            );
        }

        for (var e : config.moduleConf.reads.entrySet()) {
            controller.addReads(
                    layer.findModule(e.getKey()).orElseThrow(),
                    layer.findModule(e.getValue()).orElseThrow()
            );
        }

        Module mainModule = layer.findModule(config.moduleConf.mainModule).orElseThrow();

        Module unnamed = ModuleLaunch.class.getClassLoader().getUnnamedModule();
        if (unnamed != null) {
            controller.addOpens(mainModule, getPackageFromClass(config.mainclass), unnamed);
        }

        // Start main class
        MethodHandles.lookup()
                .findStatic(Class.forName(mainClass, true, mainModule.getClassLoader()), "main", MethodType.methodType(void.class, String[].class))
                .invoke(args);
    }

    private static String getPackageFromClass(String clazz) {
        int index = clazz.lastIndexOf(".");
        if (index >= 0) {
            return clazz.substring(0, index);
        }

        return clazz;
    }
}
