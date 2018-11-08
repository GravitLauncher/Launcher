package ru.gravit.launcher.server;

import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.jar.JarFile;

public class ServerAgent {
    private static boolean isAgentStarted = false;
    public static Instrumentation inst;

    public static final class StarterVisitor extends SimpleFileVisitor<Path> {
        private Instrumentation inst;

        public StarterVisitor(Instrumentation inst) {
            this.inst = inst;
        }

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

    public boolean isAgentStarted() {
        return isAgentStarted;
    }

    public static long getObjSize(Object obj) {
        return inst.getObjectSize(obj);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        LogHelper.debug("Server Agent");
        inst = instrumentation;
        isAgentStarted = true;

        try {
            Files.walkFileTree(Paths.get("libraries"), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new StarterVisitor(inst));
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
