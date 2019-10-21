package pro.gravit.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;

import pro.gravit.utils.helper.LogHelper;

@LauncherAPI
public final class LauncherAgent {
    private static boolean isAgentStarted = false;
    public static Instrumentation inst;

    public static void addJVMClassPath(String path) throws IOException {
        LogHelper.debug("Launcher Agent addJVMClassPath");
        inst.appendToSystemClassLoaderSearch(new JarFile(new File(path)));
    }
    
    public static void addJVMClassPath(Path path) throws IOException {
        LogHelper.debug("Launcher Agent addJVMClassPath");
        inst.appendToSystemClassLoaderSearch(new JarFile(path.toFile()));
    }

    public boolean isAgentStarted() {
        return isAgentStarted;
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        System.out.println("Launcher Agent");
        checkAgentStacktrace();
        inst = instrumentation;
        //SafeExitJVMLegacy.class.getName();
        //SafeExitJVM.class.getName();
        //NativeJVMHalt.class.getName();
        //NativeJVMHalt.initFunc();
        isAgentStarted = true;
    }
    public static void checkAgentStacktrace()
    {
        RuntimeException ex = new SecurityException("Error check agent stacktrace");
        boolean isFoundNative = false;
        boolean foundPreMain = false;
        for(StackTraceElement e : ex.getStackTrace())
        {
            if(e.isNativeMethod())
            {
                if(!isFoundNative) isFoundNative = true;
                else throw ex;
            }
            if(e.getMethodName().equals("premain"))
            {
                if(!foundPreMain) foundPreMain = true;
                else throw ex;
            }
        }
        if(!isFoundNative || !foundPreMain) throw ex;
    }

    public static boolean isStarted() {
        return isAgentStarted;
    }
}
