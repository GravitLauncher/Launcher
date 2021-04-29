package pro.gravit.launcher.api;

import java.lang.instrument.Instrumentation;
import java.net.URL;

public class ClientService {
    public static Instrumentation instrumentation;
    public static ClassLoader classLoader;
    public static String nativePath;
    public static URL[] baseURLs;

    public static ClassLoader getClassLoader() {
        return classLoader;
    }
}
