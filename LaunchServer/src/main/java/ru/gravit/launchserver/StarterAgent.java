package ru.gravit.launchserver;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
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

    public static Instrumentation inst = null;
    public static Path libraries = null;
    private static boolean isStarted = false;

    public static boolean isAgentStarted() {
        return isStarted;
    }

    public static void premain(String agentArgument, Instrumentation inst) {
        StarterAgent.inst = inst;
        isStarted = true;
        try {
        	libraries = Paths.get("libraries");
        	if (ManagementFactory.getOperatingSystemMXBean().getName().startsWith("Linux"))
        		fixLibsPerms();
            Files.walkFileTree(libraries, Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new StarterVisitor(inst));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }

	private static void fixLibsPerms() throws InterruptedException, IOException {
		Path file = libraries.resolve(".libraries_chmoded");
		if (Files.exists(file)) return;
		Files.deleteIfExists(file);
		Runtime.getRuntime().exec(new String[] {"chmod", "-R", "+x", "libraries"}, null, libraries.toAbsolutePath().normalize().toFile().getParentFile()).waitFor();
		Files.createFile(file);
	}
}
