package pro.gravit.launcher.client;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ClientClassLoader extends URLClassLoader {
    private static final ClassLoader SYSTEM_CLASS_LOADER = ClassLoader.getSystemClassLoader();
    public String nativePath;

    private final List<String> packages = new ArrayList<>();

    /**
     * Constructs a new URLClassLoader for the specified URLs using the
     * default delegation parent {@code ClassLoader}. The URLs will
     * be searched in the order specified for classes and resources after
     * first searching in the parent class loader. Any URL that ends with
     * a '/' is assumed to refer to a directory. Otherwise, the URL is
     * assumed to refer to a JAR file which will be downloaded and opened
     * as needed.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param urls the URLs from which to load classes and resources
     * @throws SecurityException    if a security manager exists and its
     *                              {@code checkCreateClassLoader} method doesn't allow
     *                              creation of a class loader.
     * @throws NullPointerException if {@code urls} is {@code null}.
     */
    public ClientClassLoader(URL[] urls) {
        super(urls);
        packages.add("pro.gravit.launcher.");
        packages.add("pro.gravit.utils.");
    }

    /**
     * Constructs a new URLClassLoader for the given URLs. The URLs will be
     * searched in the order specified for classes and resources after first
     * searching in the specified parent class loader.  Any {@code jar:}
     * scheme URL is assumed to refer to a JAR file.  Any {@code file:} scheme
     * URL that ends with a '/' is assumed to refer to a directory.  Otherwise,
     * the URL is assumed to refer to a JAR file which will be downloaded and
     * opened as needed.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param urls   the URLs from which to load classes and resources
     * @param parent the parent class loader for delegation
     * @throws SecurityException    if a security manager exists and its
     *                              {@code checkCreateClassLoader} method doesn't allow
     *                              creation of a class loader.
     * @throws NullPointerException if {@code urls} is {@code null}.
     */
    public ClientClassLoader(URL[] urls, ClassLoader parent) {
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
    public String findLibrary(String name) {
        return nativePath.concat(IOHelper.PLATFORM_SEPARATOR).concat(JVMHelper.NATIVE_PREFIX).concat(name).concat(JVMHelper.NATIVE_EXTENSION);
    }

    public void addAllowedPackage(String pkg) {
        packages.add(pkg);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}

