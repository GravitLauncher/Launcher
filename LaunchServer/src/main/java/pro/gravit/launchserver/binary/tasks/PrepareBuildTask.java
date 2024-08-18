package pro.gravit.launchserver.binary.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.UnpackHelper;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrepareBuildTask implements LauncherBuildTask {
    private final LaunchServer server;
    private final Path result;
    private transient final Logger logger = LogManager.getLogger();

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
        server.launcherBinary.addonLibs.clear();
        server.launcherBinary.files.clear();
        IOHelper.walk(server.launcherLibraries, new ListFileVisitor(server.launcherBinary.coreLibs), false);
        if(Files.isDirectory(server.launcherLibrariesCompile)) {
            IOHelper.walk(server.launcherLibrariesCompile, new ListFileVisitor(server.launcherBinary.addonLibs), false);
        }
        try(Stream<Path> stream = Files.walk(server.launcherPack, FileVisitOption.FOLLOW_LINKS).filter((e) -> {
            try {
                return !Files.isDirectory(e) && !Files.isHidden(e);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        })) {
            var map = stream.collect(Collectors.toMap(k -> server.launcherPack.relativize(k).toString().replace("\\", "/"), (v) -> v));
            server.launcherBinary.files.putAll(map);
        }
        UnpackHelper.unpack(IOHelper.getResourceURL("Launcher.jar"), result);
        tryUnpack();
        return result;
    }

    public void tryUnpack() throws IOException {
        logger.info("Unpacking launcher native guard list and runtime");
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
