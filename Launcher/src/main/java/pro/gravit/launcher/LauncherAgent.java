package pro.gravit.launcher;

import pro.gravit.launcher.patches.FMLPatcher;
import pro.gravit.launcher.utils.NativeJVMHalt;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;


public final class LauncherAgent {
    public static Instrumentation inst;
    private static boolean isAgentStarted = false;

    public static void addJVMClassPath(String path) throws IOException {
        LogHelper.debug("Launcher Agent addJVMClassPath");
        inst.appendToSystemClassLoaderSearch(new JarFile(new File(path)));
    }

    public static void addJVMClassPath(Path path) throws IOException {
        LogHelper.debug("Launcher Agent addJVMClassPath");
        inst.appendToSystemClassLoaderSearch(new JarFile(path.toFile()));
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        System.out.println("Launcher Agent");
        checkAgentStacktrace();
        inst = instrumentation;
        NativeJVMHalt.initFunc();
        FMLPatcher.apply();
        isAgentStarted = true;
    }

    public static void checkAgentStacktrace() {
        RuntimeException ex = new SecurityException("Error check agent stacktrace");
        boolean isFoundNative = false;
        boolean foundPreMain = false;
        for (StackTraceElement e : ex.getStackTrace()) {
            if (e.isNativeMethod()) {
                if (!isFoundNative) isFoundNative = true;
                else throw ex;
            }
            if (e.getMethodName().equals("premain")) {
                if (!foundPreMain) foundPreMain = true;
                else throw ex;
            }
        }
        if (!isFoundNative || !foundPreMain) throw ex;
    }

    public static boolean isStarted() {
        return isAgentStarted;
    }

    public boolean isAgentStarted() {
        return isAgentStarted;
    }
}
