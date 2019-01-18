package ru.gravit.launchserver.binary.tasks;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.UnpackHelper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class PrepareBuildTask implements LauncherBuildTask {
    private final LaunchServer server;
    private final Path result;

    public PrepareBuildTask(LaunchServer server) {
        this.server = server;
        result = server.launcherBinary.buildDir.resolve("Launcher-clean.jar");
    }

    @Override
    public String getName() {
        return "UnpackFromResources";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        server.launcherBinary.coreLibs.clear();
        IOHelper.walk(server.launcherLibraries, new ListFileVisitor(server.launcherBinary.coreLibs), true);
        UnpackHelper.unpack(IOHelper.getResourceURL("Launcher.jar"), result);
        tryUnpack();
        return result;
    }

    @Override
    public boolean allowDelete() {
        return false;
    }

    public void tryUnpack() throws IOException {
        LogHelper.info("Unpacking launcher native guard files and runtime");
        UnpackHelper.unpackZipNoCheck("guard.zip", server.launcherBinary.guardDir);
        UnpackHelper.unpackZipNoCheck("runtime.zip", server.launcherBinary.runtimeDir);
    }

    private static final class ListFileVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> lst;

        private ListFileVisitor(List<Path> lst) {
            this.lst = lst;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!Files.isDirectory(file) && file.toFile().getName().endsWith(".jar"))
                lst.add(file);
            return super.visitFile(file, attrs);
        }
    }
}
