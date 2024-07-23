package pro.gravit.launcher.base.api;

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
        if(name == null) {
            return null;
        }
        var needExt = !name.endsWith(JVMHelper.NATIVE_EXTENSION);
        var needPrefix = !name.startsWith(JVMHelper.NATIVE_PREFIX);
        if(needExt && needPrefix) {
            return nativePath.concat(IOHelper.PLATFORM_SEPARATOR).concat(JVMHelper.NATIVE_PREFIX).concat(name).concat(JVMHelper.NATIVE_EXTENSION);
        } else if(needExt) {
            return nativePath.concat(IOHelper.PLATFORM_SEPARATOR).concat(name).concat(JVMHelper.NATIVE_EXTENSION);
        } else if(needPrefix) {
            return nativePath.concat(IOHelper.PLATFORM_SEPARATOR).concat(JVMHelper.NATIVE_PREFIX).concat(name);
        } else {
            return nativePath.concat(IOHelper.PLATFORM_SEPARATOR).concat(name);
        }
    }
}
