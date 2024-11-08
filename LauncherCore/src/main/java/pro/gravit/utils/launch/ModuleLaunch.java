package pro.gravit.utils.launch;

import pro.gravit.utils.helper.HackHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleLaunch implements Launch {
    private ModuleClassLoader moduleClassLoader;
    private Configuration configuration;
    private ModuleLayer.Controller controller;
    private ModuleFinder moduleFinder;
    private ModuleLayer layer;
    private MethodHandles.Lookup hackLookup;
    private boolean disablePackageDelegateSupport;
    @Override
    public ClassLoaderControl init(List<Path> files, String nativePath, LaunchOptions options) {
        this.disablePackageDelegateSupport = options.disablePackageDelegateSupport;
        moduleClassLoader = new ModuleClassLoader(files.stream().map((e) -> {
            try {
                return e.toUri().toURL();
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }).toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
        moduleClassLoader.nativePath = nativePath;
        {
            if(options.enableHacks) {
                hackLookup = HackHelper.createHackLookup(ModuleLaunch.class);
            }
            if(options.moduleConf != null) {
                // Create Module Layer
                moduleFinder = ModuleFinder.of(options.moduleConf.modulePath.stream().map(Paths::get).map(Path::toAbsolutePath).toArray(Path[]::new));
                ModuleLayer bootLayer = ModuleLayer.boot();
                if(options.moduleConf.modules.contains("ALL-MODULE-PATH")) {
                    var set = moduleFinder.findAll();
                    if(LogHelper.isDevEnabled()) {
                        for(var m : set) {
                            LogHelper.dev("Found module %s in %s", m.descriptor().name(), m.location().map(URI::toString).orElse("unknown"));
                        }
                        LogHelper.dev("Found %d modules", set.size());
                    }
                    for(var m : set) {
                        options.moduleConf.modules.add(m.descriptor().name());
                    }
                    options.moduleConf.modules.remove("ALL-MODULE-PATH");
                }
                configuration = bootLayer.configuration()
                        .resolveAndBind(moduleFinder, ModuleFinder.of(), options.moduleConf.modules);
                controller = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(bootLayer), moduleClassLoader);
                layer = controller.layer();
                // Configure exports / opens
                for(var e : options.moduleConf.exports.entrySet()) {
                    String[] split = e.getKey().split("/");
                    String moduleName = split[0];
                    String pkg = split[1];
                    LogHelper.dev("Export module: %s package: %s to %s", moduleName, pkg, e.getValue());
                    Module source = layer.findModule(split[0]).orElse(null);
                    if(source == null) {
                        throw new RuntimeException(String.format("Module %s not found", moduleName));
                    }
                    Module target = layer.findModule(e.getValue()).orElse(null);
                    if(target == null) {
                        throw new RuntimeException(String.format("Module %s not found", e.getValue()));
                    }
                    if(options.enableHacks && source.getLayer() != layer) {
                        ModuleHacks.createController(hackLookup, source.getLayer()).addExports(source, pkg, target);
                    } else {
                        controller.addExports(source, pkg, target);
                    }
                }
                for(var e : options.moduleConf.opens.entrySet()) {
                    String[] split = e.getKey().split("/");
                    String moduleName = split[0];
                    String pkg = split[1];
                    LogHelper.dev("Open module: %s package: %s to %s", moduleName, pkg, e.getValue());
                    Module source = layer.findModule(split[0]).orElse(null);
                    if(source == null) {
                        throw new RuntimeException(String.format("Module %s not found", moduleName));
                    }
                    Module target = layer.findModule(e.getValue()).orElse(null);
                    if(target == null) {
                        throw new RuntimeException(String.format("Module %s not found", e.getValue()));
                    }
                    if(options.enableHacks && source.getLayer() != layer) {
                        ModuleHacks.createController(hackLookup, source.getLayer()).addOpens(source, pkg, target);
                    } else {
                        controller.addOpens(source, pkg, target);
                    }
                }
                for(var e : options.moduleConf.reads.entrySet()) {
                    LogHelper.dev("Read module %s to %s", e.getKey(), e.getValue());
                    Module source = layer.findModule(e.getKey()).orElse(null);
                    if(source == null) {
                        throw new RuntimeException(String.format("Module %s not found", e.getKey()));
                    }
                    Module target = layer.findModule(e.getValue()).orElse(null);
                    if(target == null) {
                        throw new RuntimeException(String.format("Module %s not found", e.getValue()));
                    }
                    if(options.enableHacks && source.getLayer() != layer) {
                        ModuleHacks.createController(hackLookup, source.getLayer()).addReads(source, target);
                    } else {
                        controller.addReads(source, target);
                    }
                }
                moduleClassLoader.initializeWithLayer(layer);
            }
        }
        return moduleClassLoader.makeControl();
    }

    @Override
    public void launch(String mainClass, String mainModuleName, Collection<String> args) throws Throwable {
        Thread.currentThread().setContextClassLoader(moduleClassLoader);
        if(mainModuleName == null) {
            Class<?> mainClazz = Class.forName(mainClass, true, moduleClassLoader);
            MethodHandle mainMethod = MethodHandles.lookup().findStatic(mainClazz, "main", MethodType.methodType(void.class, String[].class)).asFixedArity();
            JVMHelper.fullGC();
            mainMethod.asFixedArity().invokeWithArguments((Object) args.toArray(new String[0]));
            return;
        }
        Module mainModule = layer.findModule(mainModuleName).orElseThrow();
        Module unnamed = ModuleLaunch.class.getClassLoader().getUnnamedModule();
        if(unnamed != null) {
            controller.addOpens(mainModule, getPackageFromClass(mainClass), unnamed);
        }
        // Start main class
        ClassLoader loader = mainModule.getClassLoader();
        Class<?> mainClazz = Class.forName(mainClass, true, loader);
        MethodHandle mainMethod = MethodHandles.lookup().findStatic(mainClazz, "main", MethodType.methodType(void.class, String[].class));
        mainMethod.asFixedArity().invokeWithArguments((Object) args.toArray(new String[0]));
    }

    private static String getPackageFromClass(String clazz) {
        int index = clazz.lastIndexOf(".");
        if(index >= 0) {
            return clazz.substring(0, index);
        }
        return clazz;
    }

    private class ModuleClassLoader extends URLClassLoader {
        private final ClassLoader SYSTEM_CLASS_LOADER = ClassLoader.getSystemClassLoader();
        private final List<ClassLoaderControl.ClassTransformer> transformers = new ArrayList<>();
        private final Map<String, Class<?>> classMap = new ConcurrentHashMap<>();
        private final Map<String, Module> packageToModule = new HashMap<>();
        private String nativePath;

        private final List<String> packages = new ArrayList<>();

        static  {
            ClassLoader.registerAsParallelCapable();
        }

        public ModuleClassLoader(URL[] urls, ClassLoader parent) {
            super("LAUNCHER", urls, parent);
            packages.add("pro.gravit.launcher.");
            packages.add("pro.gravit.utils.");
        }

        private void initializeWithLayer(ModuleLayer layer) {
            for(var m : layer.modules()) {
                for(var p : m.getPackages()) {
                    packageToModule.put(p, m);
                }
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if(name != null && !disablePackageDelegateSupport) {
                for(String pkg : packages) {
                    if(name.startsWith(pkg)) {
                        return SYSTEM_CLASS_LOADER.loadClass(name);
                    }
                }
            }
            return super.loadClass(name, resolve);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            var clazz = findClass(null, name);
            if(clazz == null) {
                throw new ClassNotFoundException(name);
            }
            return clazz;
        }

        @Override
        protected Class<?> findClass(String moduleName, String name) {
            Class<?> clazz;
            {
                clazz = classMap.get(name);
                if(clazz != null) {
                    return clazz;
                }
            }
            if(name != null && !transformers.isEmpty()) {
                boolean needTransform = false;
                for(ClassLoaderControl.ClassTransformer t : transformers) {
                    if(t.filter(moduleName, name)) {
                        needTransform = true;
                        break;
                    }
                }
                if(needTransform) {
                    String rawClassName = name.replace(".", "/").concat(".class");
                    try(InputStream input = getResourceAsStream(rawClassName)) {
                        byte[] bytes = IOHelper.read(input);
                        for(ClassLoaderControl.ClassTransformer t : transformers) {
                            bytes = t.transform(moduleName, name, null, bytes);
                        }
                        clazz = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        return null;
                    }
                }
            }
            if(clazz == null && layer != null && name != null) {
                var pkg = getPackageFromClass(name);
                var module = packageToModule.get(pkg);
                if(module != null) {
                    try {
                        clazz = module.getClassLoader().loadClass(name);
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
            }
            if(clazz == null) {
                try {
                    clazz = super.findClass(name);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
            if(clazz != null) {
                classMap.put(name, clazz);
                return clazz;
            } else {
                return null;
            }
        }

        @Override
        public String findLibrary(String name) {
            if(nativePath == null) {
                return null;
            }
            return nativePath.concat(IOHelper.PLATFORM_SEPARATOR).concat(JVMHelper.NATIVE_PREFIX).concat(name).concat(JVMHelper.NATIVE_EXTENSION);
        }

        public void addAllowedPackage(String pkg) {
            packages.add(pkg);
        }

        public void clearAllowedPackages() {
            packages.clear();
        }

        private ModuleClassLoaderControl makeControl() {
            return new ModuleClassLoaderControl();
        }

        private class ModuleClassLoaderControl implements ClassLoaderControl {

            @Override
            public void addLauncherPackage(String prefix) {
                addAllowedPackage(prefix);
            }

            @Override
            public void clearLauncherPackages() {
                clearAllowedPackages();
            }

            @Override
            public void addTransformer(ClassTransformer transformer) {
                transformers.add(transformer);
            }

            @Override
            public void addURL(URL url) {
                ModuleClassLoader.this.addURL(url);
            }

            @Override
            public void addJar(Path path) {
                try {
                    ModuleClassLoader.this.addURL(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public URL[] getURLs() {
                return ModuleClassLoader.this.getURLs();
            }

            @Override
            public Class<?> getClass(String name) throws ClassNotFoundException {
                return Class.forName(name, false, ModuleClassLoader.this);
            }

            @Override
            public ClassLoader getClassLoader() {
                return ModuleClassLoader.this;
            }

            @Override
            public Object getJava9ModuleController() {
                return controller;
            }

            @Override
            public MethodHandles.Lookup getHackLookup() {
                return hackLookup;
            }
        }
    }
}
