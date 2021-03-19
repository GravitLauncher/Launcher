package pro.gravit.launcher.server;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarFile;

public class ServerAgent {
    public static final Boolean isAutoloadLibraries = Boolean.getBoolean(System.getProperty("serverwrapper,agentlibrariesload", "false"));
    public static Instrumentation inst = null;
    private static boolean isAgentStarted = false;

    public static void addJVMClassPath(String path) throws IOException {
        LogHelper.debug("Load %s", path);
        inst.appendToSystemClassLoaderSearch(new JarFile(path));
    }

    public static void addJVMClassPath(JarFile file) {
        LogHelper.debug("Load %s", file.getName());
        inst.appendToSystemClassLoaderSearch(file);
    }

    public static boolean isAgentStarted() {
        return isAgentStarted;
    }

    public static long getObjSize(Object obj) {
        return inst.getObjectSize(obj);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        LogHelper.debug("Server Agent");
        inst = instrumentation;
        isAgentStarted = true;
        if (isAutoloadLibraries) {
            Path libraries = Paths.get("libraries");
            if (IOHelper.exists(libraries)) loadLibraries(libraries);
        }
        String proxyClassName = System.getProperty("serverwrapper.agentproxy", null);
        if (proxyClassName != null) {
            Class<?> proxyClass;
            try {
                proxyClass = Class.forName(proxyClassName);
                MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(proxyClass, "premain", MethodType.methodType(void.class, String.class, Instrumentation.class));
                mainMethod.invoke(agentArgument, instrumentation);
            } catch (Throwable e) {
                LogHelper.error(e);
            }
        }
    }

    public static void loadLibraries(Path dir) {
        try {
            IOHelper.walk(dir, new StarterVisitor(), true);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    private static final class StarterVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar")) addJVMClassPath(new JarFile(file.toFile()));
            return super.visitFile(file, attrs);
        }
    }
}
