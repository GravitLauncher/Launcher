package ru.gravit.launcher;

import ru.gravit.utils.helper.LogHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

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
        boolean pb = true;
        boolean rt = true;
        if (agentArgument != null) {
        	String trimmedArg = agentArgument.trim();
        	if (!trimmedArg.isEmpty())  {
        		if (trimmedArg.contains("p")) pb = false;
        		if (trimmedArg.contains("r")) rt = false;
        	}
        }
    }

    public static boolean isStarted() {
        return isAgentStarted;
    }
}
