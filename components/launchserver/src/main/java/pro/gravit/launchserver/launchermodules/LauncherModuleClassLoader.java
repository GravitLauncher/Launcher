package pro.gravit.launchserver.launchermodules;

import java.net.URL;
import java.net.URLClassLoader;

public class LauncherModuleClassLoader extends URLClassLoader {
    public LauncherModuleClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public void addURL(URL u) {
        super.addURL(u);
    }

    public Class<?> rawDefineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
