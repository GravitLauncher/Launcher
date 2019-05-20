package ru.gravit.launchserver;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public final class StarterAgent {

    private static final class StarterVisitor extends SimpleFileVisitor<Path> {
        private static final Set<PosixFilePermission> DPERMS;

        static {
            Set<PosixFilePermission> perms = new HashSet<>(Arrays.asList(PosixFilePermission.values()));
            perms.remove(PosixFilePermission.OTHERS_WRITE);
            perms.remove(PosixFilePermission.GROUP_WRITE);
            DPERMS = Collections.unmodifiableSet(perms);
        }

        private final Path filef;
        private final boolean fixLib;

        private StarterVisitor() {
            this.filef = StarterAgent.libraries.resolve(".libraries_chmoded");
            this.fixLib = !Files.exists(filef) && !Boolean.getBoolean("launcher.noLibrariesPosixPermsFix");
            if (fixLib) {
                try {
                    Files.deleteIfExists(filef);
                    Files.createFile(filef);
                } catch (Throwable ignored) {
                }
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (fixLib && Files.getFileAttributeView(file, PosixFileAttributeView.class) != null)
                Files.setPosixFilePermissions(file, DPERMS);
            if (file.toFile().getName().endsWith(".jar"))
                inst.appendToSystemClassLoaderSearch(new JarFile(file.toFile()));
            return super.visitFile(file, attrs);
        }
    }

    public static Instrumentation inst = null;
    public static Path libraries = null;
    private static boolean isStarted = false;

    public static boolean isAgentStarted() {
        return isStarted;
    }

    public static void premain(String agentArgument, Instrumentation inst) {
        StarterAgent.inst = inst;
        libraries = Paths.get("libraries");
        isStarted = true;
        try {
            Files.walkFileTree(libraries, Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new StarterVisitor());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
