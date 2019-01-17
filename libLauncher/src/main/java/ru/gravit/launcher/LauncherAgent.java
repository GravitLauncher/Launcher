package ru.gravit.launcher;

import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

@LauncherAPI
public final class LauncherAgent {
    private static boolean isAgentStarted = false;
    public static Instrumentation inst;

    public static void addJVMClassPath(String path) throws IOException {
        LogHelper.debug("Launcher Agent addJVMClassPath");
        inst.appendToSystemClassLoaderSearch(new JarFile(path));
    }

    public boolean isAgentStarted() {
        return isAgentStarted;
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        System.out.println("Launcher Agent");
        inst = instrumentation;
        isAgentStarted = true;
    }

    public static boolean isStarted() {
        return isAgentStarted;
    }
}
