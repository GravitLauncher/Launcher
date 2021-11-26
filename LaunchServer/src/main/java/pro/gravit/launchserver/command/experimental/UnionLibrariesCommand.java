package pro.gravit.launchserver.command.experimental;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class UnionLibrariesCommand extends Command {
    private final Logger logger = LogManager.getLogger();
    public UnionLibrariesCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "union libraries";
    }

    @Override
    public void invoke(String... args) throws Exception {
        Map<Path, List<Path>> libraries = new HashMap<>();
        logger.info("Resolve libraries");
        Path librariesDir = server.updatesDir.resolve("libraries");
        for(String dir : server.updatesManager.getUpdatesList()) {
            var visitor = new ListFileVisitor();
            var updateDir = server.updatesDir.resolve(dir).resolve("libraries");
            if(Files.notExists(updateDir)) {
                continue;
            }
            IOHelper.walk(updateDir, visitor, false);
            var list = visitor.getList();
            for(Path p : list) {
                var p1 = updateDir.relativize(p);
                var l = libraries.computeIfAbsent(p1, k -> new ArrayList<>(8));
                l.add(p);
            }
        }
        if(Files.notExists(librariesDir)) {
            Files.createDirectories(librariesDir);
        }
        logger.info("Move libraries");
        libraries.forEach((k,v) -> {
            ClientProfile.ClientProfileLibrary library;
            String name = ClientProfile.ClientProfileLibrary.convertMavenPathToName(k.toString());
            if(name == null) {
                name = k.getFileName().toString();
            }
            if(v.size() == 1) {
                library = new ClientProfile.ClientProfileLibrary("", name, k.toString());
            } else {
                library = new ClientProfile.ClientProfileLibrary("libraries", name, k.toString());
            }
            try {
                Path target = librariesDir.resolve(k);
                Path source = v.get(0);
                if(v.size() > 1 && Files.notExists(target)) {
                    IOHelper.createParentDirs(target);
                    logger.debug("Move {} to {}", source, target);
                    Files.move(source, target);
                }
                for(Path p : v) {
                    var dirName = server.updatesDir.relativize(p).getName(0).toString();
                    logger.debug("dirName {}", dirName);
                    for(ClientProfile profile : server.getProfiles()) {
                        if(profile.getDir().equals(dirName)) {
                            logger.debug("Found profile {}", profile.getTitle());
                            var list = profile.getLibraries();
                            list.add(library);
                        }
                    }
                    if(p != source) {
                        logger.debug("Delete {}", p);
                        Files.delete(p);
                    }
                }
                logger.info("Library {} found {} duplicated", k, v.size());
            } catch (IOException e) {
                logger.error(e);
            }
        });
        logger.info("Write profiles");
        for(ClientProfile profile : server.getProfiles()) {
            try (Writer w = IOHelper.newWriter(server.profilesDir.resolve(profile.getTitle().concat(".json")))) {
                Launcher.gsonManager.configGson.toJson(profile, w);
            }
        }
        server.syncProfilesDir();
        server.updatesManager.syncUpdatesDir(null);
    }

    public static class ListFileVisitor extends SimpleFileVisitor<Path> {
        private final List<Path> list = new LinkedList<>();
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            list.add(file);
            return super.visitFile(file, attrs);
        }

        public List<Path> getList() {
            return list;
        }
    }
}
