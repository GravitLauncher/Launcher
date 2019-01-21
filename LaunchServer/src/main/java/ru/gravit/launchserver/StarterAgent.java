package ru.gravit.launchserver;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.jar.JarFile;

public final class StarterAgent {

    private static final class StarterVisitor extends SimpleFileVisitor<Path> {
        private final Instrumentation inst;

        private StarterVisitor(Instrumentation inst) {
            this.inst = inst;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar"))
                inst.appendToSystemClassLoaderSearch(new JarFile(file.toFile()));
            return super.visitFile(file, attrs);
        }
    }

    public static Instrumentation inst;
    private static boolean isStarted = false;

    public static boolean isAgentStarted() {
        return isStarted;
    }

    public static void premain(String agentArgument, Instrumentation inst) {
        StarterAgent.inst = inst;
        isStarted = true;
        try {
            Files.walkFileTree(Paths.get("libraries"), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new StarterVisitor(inst));
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
