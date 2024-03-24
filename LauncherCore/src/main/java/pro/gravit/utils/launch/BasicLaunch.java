package pro.gravit.utils.launch;

import pro.gravit.utils.helper.HackHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

public class BasicLaunch implements Launch {

    private Instrumentation instrumentation;
    private MethodHandles.Lookup hackLookup;

    public BasicLaunch(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public BasicLaunch() {
    }

    @Override
    public ClassLoaderControl init(List<Path> files, String nativePath, LaunchOptions options) {
        if(options.enableHacks) {
            hackLookup = HackHelper.createHackLookup(BasicLaunch.class);
        }
        return new BasicClassLoaderControl();
    }

    @Override
    public void launch(String mainClass, String mainModule, Collection<String> args) throws Throwable {
        Class<?> mainClazz = Class.forName(mainClass);
        MethodHandle mainMethod = MethodHandles.lookup().findStatic(mainClazz, "main", MethodType.methodType(void.class, String[].class)).asFixedArity();
        JVMHelper.fullGC();
        mainMethod.asFixedArity().invokeWithArguments((Object) args.toArray(new String[0]));
    }

    private class BasicClassLoaderControl implements ClassLoaderControl {

        @Override
        public void addLauncherPackage(String prefix) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearLauncherPackages() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addTransformer(ClassTransformer transformer) {
            if (instrumentation == null) {
                throw new UnsupportedOperationException();
            }
            instrumentation.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    if(transformer.filter(null, className)) {
                        return transformer.transform(null, className, protectionDomain, classfileBuffer);
                    }
                    return classfileBuffer;
                }
            });
        }

        @Override
        public void addURL(URL url) {
            if (instrumentation == null) {
                throw new UnsupportedOperationException();
            }
            try {
                instrumentation.appendToSystemClassLoaderSearch(new JarFile(new File(url.toURI())));
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void addJar(Path path) {
            if (instrumentation == null) {
                throw new UnsupportedOperationException();
            }
            try {
                instrumentation.appendToSystemClassLoaderSearch(new JarFile(path.toFile()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public URL[] getURLs() {
            String classpath = System.getProperty("java.class.path");
            String[] split = classpath.split(File.pathSeparator);
            URL[] urls = new URL[split.length];
            try {
                for(int i=0;i<split.length;i++) {
                    urls[i] = Paths.get(split[i]).toAbsolutePath().toUri().toURL();
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            return urls;
        }

        @Override
        public Class<?> getClass(String name) throws ClassNotFoundException {
            return Class.forName(name);
        }

        @Override
        public ClassLoader getClassLoader() {
            return BasicLaunch.class.getClassLoader();
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
