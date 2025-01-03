package pro.gravit.utils.launch;

import pro.gravit.utils.helper.HackHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LegacyLaunch implements Launch {
    private LegacyClassLoader legacyClassLoader;
    private MethodHandles.Lookup hackLookup;

    @Override
    public ClassLoaderControl init(List<Path> files, String nativePath, LaunchOptions options) {
        legacyClassLoader = new LegacyClassLoader(files.stream().map((e) -> {
            try {
                return e.toUri().toURL();
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }).toArray(URL[]::new), BasicLaunch.class.getClassLoader());
        legacyClassLoader.nativePath = nativePath;
        if(options.enableHacks) {
            hackLookup = HackHelper.createHackLookup(BasicLaunch.class);
        }
        return legacyClassLoader.makeControl();
    }

    @Override
    public void launch(String mainClass, String mainModule, Collection<String> args) throws Throwable {
        Thread.currentThread().setContextClassLoader(legacyClassLoader);
        Class<?> mainClazz = Class.forName(mainClass, true, legacyClassLoader);
        MethodHandle mainMethod = MethodHandles.lookup().findStatic(mainClazz, "main", MethodType.methodType(void.class, String[].class)).asFixedArity();
        JVMHelper.fullGC();
        mainMethod.asFixedArity().invokeWithArguments((Object) args.toArray(new String[0]));
    }

    private class LegacyClassLoader extends URLClassLoader {
        private final ClassLoader SYSTEM_CLASS_LOADER = ClassLoader.getSystemClassLoader();
        private final List<ClassLoaderControl.ClassTransformer> transformers = new ArrayList<>();
        private final Map<String, Class<?>> classMap = new ConcurrentHashMap<>();
        private String nativePath;

        static  {
            ClassLoader.registerAsParallelCapable();
        }

        private final List<String> packages = new ArrayList<>();
        public LegacyClassLoader(URL[] urls) {
            super(urls);
            packages.add("pro.gravit.launcher.");
            packages.add("pro.gravit.utils.");
        }
        public LegacyClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if(name != null) {
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
                    if(t.filter(null, name)) {
                        needTransform = true;
                        break;
                    }
                }
                if(needTransform) {
                    String rawClassName = name.replace(".", "/").concat(".class");
                    try(InputStream input = getResourceAsStream(rawClassName)) {
                        byte[] bytes = IOHelper.read(input);
                        for(ClassLoaderControl.ClassTransformer t : transformers) {
                            bytes = t.transform(null, name, null, bytes);
                        }
                        clazz = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
            }
            if(clazz == null) {
                clazz = super.findClass(name);
            }
            if(clazz != null) {
                classMap.put(name, clazz);
                return clazz;
            } else {
                throw new ClassNotFoundException(name);
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

        private LegacyClassLoaderControl makeControl() {
            return new LegacyClassLoaderControl();
        }

        public class LegacyClassLoaderControl implements ClassLoaderControl {

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
                LegacyClassLoader.this.addURL(url);
            }

            @Override
            public void addJar(Path path) {
                try {
                    LegacyClassLoader.this.addURL(path.toUri().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public URL[] getURLs() {
                return LegacyClassLoader.this.getURLs();
            }

            @Override
            public Class<?> getClass(String name) throws ClassNotFoundException {
                return Class.forName(name, false, LegacyClassLoader.this);
            }

            @Override
            public ClassLoader getClassLoader() {
                return LegacyClassLoader.this;
            }

            @Override
            public Object getJava9ModuleController() {
                return null;
            }

            @Override
            public MethodHandles.Lookup getHackLookup() {
                return hackLookup;
            }
        }
    }
}
