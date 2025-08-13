package pro.gravit.utils.launch;

import pro.gravit.utils.helper.HackHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleLaunch implements Launch {
    private ModuleClassLoader moduleClassLoader;
    private Configuration configuration;
    private ModuleLayer.Controller controller;
    private ModuleFinder moduleFinder;
    private ModuleLayer layer;
    private MethodHandles.Lookup hackLookup;
    private boolean disablePackageDelegateSupport;
    private static final MethodHandle ENABLE_NATIVE_ACCESS;

    static {
        MethodHandle mh;
        try {
            mh = MethodHandles.lookup().findVirtual(ModuleLayer.Controller.class, "enableNativeAccess", MethodType.methodType(ModuleLayer.Controller.class, Module.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            mh = null;
        }
        ENABLE_NATIVE_ACCESS = mh;
    }

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
                if(options.moduleConf.enableModularClassTransform) {
                    AtomicReference<ModuleClassLoader> clRef = new AtomicReference<>(moduleClassLoader);
                    moduleFinder = new CustomModuleFinder(moduleFinder, clRef);
                }
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
                for(var e : options.moduleConf.enableNativeAccess) {
                    LogHelper.dev("Enable Native Access %s", e);
                    Module source = layer.findModule(e).orElse(null);
                    if(source == null) {
                        throw new RuntimeException(String.format("Module %s not found", e));
                    }
                    if(ENABLE_NATIVE_ACCESS != null) {
                        try {
                            ENABLE_NATIVE_ACCESS.invoke(controller, source);
                        } catch (Throwable ex) {
                            throw new RuntimeException(ex);
                        }
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

    private class CustomModuleFinder implements ModuleFinder {
        private final ModuleFinder delegate;
        private AtomicReference<ModuleClassLoader> cl;

        public CustomModuleFinder(ModuleFinder delegate, AtomicReference<ModuleClassLoader> cl) {
            this.delegate = delegate;
            this.cl = cl;
        }

        @Override
        public Optional<ModuleReference> find(String name) {
            return delegate.find(name).map(this::makeModuleReference);
        }

        @Override
        public Set<ModuleReference> findAll() {
            return delegate.findAll().stream()
                    .map(this::makeModuleReference)
                    .collect(Collectors.toSet());
        }

        private CustomModuleReference makeModuleReference(ModuleReference x) {
            return new CustomModuleReference(x.descriptor(), x.location().orElse(null), x, cl);
        }
    }

    private class CustomModuleReference extends ModuleReference {
        private final ModuleReference delegate;
        private final AtomicReference<ModuleClassLoader> cl;

        public CustomModuleReference(ModuleDescriptor descriptor, URI location, ModuleReference delegate, AtomicReference<ModuleClassLoader> cl) {
            super(descriptor, location);
            this.delegate = delegate;
            this.cl = cl;
        }

        @Override
        public ModuleReader open() throws IOException {
            return new CustomModuleReader(delegate.open(), cl, descriptor());
        }
    }

    private class CustomModuleReader implements ModuleReader {
        private final ModuleReader delegate;
        private final AtomicReference<ModuleClassLoader> cl;
        private final ModuleDescriptor descriptor;

        public CustomModuleReader(ModuleReader delegate, AtomicReference<ModuleClassLoader> cl, ModuleDescriptor descriptor) {
            this.delegate = delegate;
            this.cl = cl;
            this.descriptor = descriptor;
        }

        @Override
        public Optional<URI> find(String name) throws IOException {
            return delegate.find(name);
        }

        @Override
        public Optional<InputStream> open(String name) throws IOException {
            ModuleClassLoader classLoader = cl.get();
            if(classLoader == null || !name.endsWith(".class")) {
                return delegate.open(name);
            }
            var inputOptional = delegate.open(name);
            if(inputOptional.isEmpty()) {
                return inputOptional;
            }
            try(ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                inputOptional.get().transferTo(output);
                var realClassName = name.replace("/", ".").substring(0, name.length()-".class".length()-1);
                byte[] bytes = classLoader.transformClass(descriptor.name(), realClassName, output.toByteArray());
                return Optional.of(new ByteArrayInputStream(bytes));
            }
        }

        @Override
        public Optional<ByteBuffer> read(String name) throws IOException {
            // TODO class transformation unimplemented
            return delegate.read(name);
        }

        @Override
        public void release(ByteBuffer bb) {
            // TODO class transformation unimplemented
            delegate.release(bb);
        }

        @Override
        public Stream<String> list() throws IOException {
            return delegate.list();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
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
                        bytes = transformClass(moduleName, name, bytes);
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

        private byte[] transformClass(String moduleName, String name, byte[] bytes) {
            for(ClassLoaderControl.ClassTransformer t : transformers) {
                if(!t.filter(moduleName, name)) {
                    continue;
                }
                bytes = t.transform(moduleName, name, null, bytes);
            }
            return bytes;
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
