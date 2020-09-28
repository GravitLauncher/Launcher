package pro.gravit.launchserver;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.jar.JarFile;

public final class StarterAgent {

    public static Instrumentation inst = null;
    public static Path libraries = null;
    private static boolean isStarted = false;

    public static boolean isAgentStarted() {
        return isStarted;
    }

    public static void premain(String agentArgument, Instrumentation inst) {
        StarterAgent.inst = inst;
        libraries = Paths.get(Optional.ofNullable(agentArgument).map(String::trim).filter(e -> !e.isEmpty()).orElse("libraries"));
        isStarted = true;
        try {
            Files.walkFileTree(libraries, Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new StarterVisitor());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private static final class StarterVisitor extends SimpleFileVisitor<Path> {
        private static final Set<PosixFilePermission> DPERMS;

        static {
            Set<PosixFilePermission> perms = new HashSet<>(Arrays.asList(PosixFilePermission.values()));
            perms.remove(PosixFilePermission.OTHERS_WRITE);
            perms.remove(PosixFilePermission.GROUP_WRITE);
            DPERMS = Collections.unmodifiableSet(perms);
        }

        private final boolean fixLib;

        private StarterVisitor() {
            Path filef = StarterAgent.libraries.resolve(".libraries_chmoded");
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
}
