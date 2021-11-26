package pro.gravit.launcher.client;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClientClassLoader extends URLClassLoader {
    public Path clientPath;
    private ClientProfile profile;

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
     * @see SecurityManager#checkCreateClassLoader
     */
    public ClientClassLoader(URL[] urls) {
        super(urls);
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
     * @see SecurityManager#checkCreateClassLoader
     */
    public ClientClassLoader(URL[] urls, ClientProfile profile, ClassLoader parent) {
        super(urls, parent);
        this.profile = profile;
    }

    public Path resolveLibrary(String name) {
        for(ClientProfile.ClientProfileLibrary library : profile.getLibraries()) {
            if(library.type == ClientProfile.ClientProfileLibrary.LibraryType.NATIVE) {
                if(library.name.equals(name)) {
                    return ClientLauncherEntryPoint.getLibraryPath(library);
                }
            }
        }
        return clientPath.resolve("natives").resolve(getNativePrefix().concat(name).concat(getNativeEx()));
    }

    @Override
    public String findLibrary(String name) {
        return resolveLibrary(name).toString();
    }

    public String getNativeEx() {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE)
            return ".dll";
        else if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX)
            return ".so";
        else if (JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX)
            return ".dylib";
        return "";
    }

    public String getNativePrefix() {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX)
            return "lib";
        else if (JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX)
            return "lib";
        return "";
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}

