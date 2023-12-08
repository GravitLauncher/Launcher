package pro.gravit.launcher.api;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.launch.ClassLoaderControl;

import java.lang.instrument.Instrumentation;
import java.net.URL;

public class ClientService {
    public static Instrumentation instrumentation;
    public static ClassLoaderControl classLoaderControl;
    public static String nativePath;
    public static URL[] baseURLs;

    public static String findLibrary(String name) {
        return nativePath.concat(IOHelper.PLATFORM_SEPARATOR).concat(JVMHelper.NATIVE_PREFIX).concat(name).concat(JVMHelper.NATIVE_EXTENSION);
    }
}
