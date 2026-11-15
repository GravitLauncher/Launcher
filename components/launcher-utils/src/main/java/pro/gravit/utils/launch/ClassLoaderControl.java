package pro.gravit.utils.launch;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
import java.security.ProtectionDomain;

public interface ClassLoaderControl {
    void addLauncherPackage(String prefix);
    void clearLauncherPackages();
    void addTransformer(ClassTransformer transformer);
    byte[] runTransformers(String moduleName, String className, byte[] bytes);

    Class<?> defineClass(String name, byte[] b, int off, int len,
                         ProtectionDomain protectionDomain);

    boolean isNeedTransform(String moduleName, String name);
    void addURL(URL url);
    void addJar(Path path);
    URL[] getURLs();

    Class<?> getClass(String name) throws ClassNotFoundException;

    ClassLoader getClassLoader();

    Object getJava9ModuleController();

    MethodHandles.Lookup getHackLookup();
    interface ClassTransformer {
        boolean filter(String moduleName, String name);
        byte[] transform(String moduleName, String name, ProtectionDomain protectionDomain, byte[] data);
    }
}
