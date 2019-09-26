package pro.gravit.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;

import cpw.mods.fml.SafeExitJVMLegacy;
import net.minecraftforge.fml.SafeExitJVM;
import pro.gravit.launcher.utils.NativeJVMHalt;
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
        inst = instrumentation;
        SafeExitJVMLegacy.class.getName();
        SafeExitJVM.class.getName();
        NativeJVMHalt.class.getName();
        NativeJVMHalt.initFunc();
        boolean bad = false;
        try {
        	for (StackTraceElement e : new Throwable().getStackTrace())
        		if (Class.forName(e.getClassName()).getClassLoader() != Runtime.class.getClassLoader() && Class.forName(e.getClassName()) != LauncherAgent.class) bad = true;
        } catch(Throwable e) { bad = true; }
        if (bad) NativeJVMHalt.haltA(-17);
        else isAgentStarted = true;
    }

    public static boolean isStarted() {
        return isAgentStarted;
    }
}
