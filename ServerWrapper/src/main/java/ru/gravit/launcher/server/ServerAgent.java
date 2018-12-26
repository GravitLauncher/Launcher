package ru.gravit.launcher.server;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.jar.JarFile;

public class ServerAgent {
    private static boolean isAgentStarted = false;
    public static Instrumentation inst;

    public static final class StarterVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar")) addJVMClassPath(new JarFile(file.toFile()));
            return super.visitFile(file, attrs);
        }
    }

    public static void addJVMClassPath(String path) throws IOException {
        LogHelper.debug("Load %s", path);
        inst.appendToSystemClassLoaderSearch(new JarFile(path));
    }

    public static void addJVMClassPath(JarFile file) throws IOException {
        LogHelper.debug("Load %s", file.getName());
        inst.appendToSystemClassLoaderSearch(file);
    }

    public static boolean isAgentStarted() {
        return isAgentStarted;
    }

    public static long getObjSize(Object obj) {
        return inst.getObjectSize(obj);
    }

    public static Boolean isAutoloadLibraries = Boolean.getBoolean(System.getProperty("serverwrapper,agentlibrariesload", "false"));
    public static Boolean isAgentProxy = Boolean.getBoolean(System.getProperty("serverwrapper,agentproxy", "false"));

    @SuppressWarnings("JavaLangInvokeHandleSignature")
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        LogHelper.debug("Server Agent");
        inst = instrumentation;
        isAgentStarted = true;
        if (isAutoloadLibraries) {
            Path libraries = Paths.get("libraries");
            if (IOHelper.exists(libraries)) loadLibraries(libraries);
        }
        if (isAgentProxy) {
            String proxyClassName = System.getProperty("serverwrapper.agentproxyclass");
            Class<?> proxyClass;
            try {
                proxyClass = Class.forName(proxyClassName);
                MethodHandle mainMethod = MethodHandles.publicLookup().findStatic(proxyClass, "premain", MethodType.methodType(void.class, String.class, Instrumentation.class));
                Object[] args = {agentArgument, instrumentation};
                mainMethod.invoke(args);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadLibraries(Path dir) {
        try {
            Files.walkFileTree(dir, Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new StarterVisitor());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
