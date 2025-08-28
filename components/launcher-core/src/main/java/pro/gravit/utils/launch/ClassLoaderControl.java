package pro.gravit.utils.launch;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
import java.security.ProtectionDomain;

public interface ClassLoaderControl {
    void addLauncherPackage(String prefix);
    void clearLauncherPackages();
    void addTransformer(ClassTransformer transformer);
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
