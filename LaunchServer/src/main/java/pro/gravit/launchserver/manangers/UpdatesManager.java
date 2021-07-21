package pro.gravit.launchserver.manangers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerUpdatesSyncEvent;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class UpdatesManager {
    private final LaunchServer server;
    private final Logger logger = LogManager.getLogger();
    private final Path cacheFile;
    private volatile Map<String, HashedDir> updatesDirMap;

    public UpdatesManager(LaunchServer server) {
        this.server = server;
        this.cacheFile = server.dir.resolve(".updates-cache");
    }

    private void writeCache(Path file) throws IOException {
        try (HOutput output = new HOutput(IOHelper.newOutput(file))) {
            output.writeLength(updatesDirMap.size(), 0);
            for (Map.Entry<String, HashedDir> entry : updatesDirMap.entrySet()) {
                output.writeString(entry.getKey(), 0);
                entry.getValue().write(output);
            }
        }
        logger.debug("Saved {} updates to cache", updatesDirMap.size());
    }

    private void readCache(Path file) throws IOException {
        Map<String, HashedDir> updatesDirMap = new HashMap<>(16);
        try (HInput input = new HInput(IOHelper.newInput(file))) {
            int size = input.readLength(0);
            for (int i = 0; i < size; ++i) {
                String name = input.readString(0);
                HashedDir dir = new HashedDir(input);
                updatesDirMap.put(name, dir);
            }
        }
        logger.debug("Found {} updates from cache", updatesDirMap.size());
        this.updatesDirMap = Collections.unmodifiableMap(updatesDirMap);
    }

    public void readUpdatesDir() throws IOException {
        if (server.config.cacheUpdates) {
            if (Files.exists(cacheFile)) {
                try {
                    readCache(cacheFile);
                    return;
                } catch (Throwable e) {
                    logger.error("Read updates cache failed", e);
                }
            }
        }
        syncUpdatesDir(null);
    }

    public void syncUpdatesDir(Collection<String> dirs) throws IOException {
        logger.info("Syncing updates dir");
        Map<String, HashedDir> newUpdatesDirMap = new HashMap<>(16);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(server.updatesDir)) {
            for (final Path updateDir : dirStream) {
                if (Files.isHidden(updateDir))
                    continue; // Skip hidden

                // Resolve name and verify is dir
                String name = IOHelper.getFileName(updateDir);
                if (!IOHelper.isDir(updateDir)) {
                    if (!IOHelper.isFile(updateDir) && Stream.of(".jar", ".exe", ".hash").noneMatch(e -> updateDir.toString().endsWith(e)))
                        logger.warn("Not update dir: '{}'", name);
                    continue;
                }

                // Add from previous map (it's guaranteed to be non-null)
                if (dirs != null && !dirs.contains(name)) {
                    HashedDir hdir = updatesDirMap.get(name);
                    if (hdir != null) {
                        newUpdatesDirMap.put(name, hdir);
                        continue;
                    }
                }

                // Sync and sign update dir
                logger.info("Syncing '{}' update dir", name);
                HashedDir updateHDir = new HashedDir(updateDir, null, true, true);
                newUpdatesDirMap.put(name, updateHDir);
            }
        }
        updatesDirMap = Collections.unmodifiableMap(newUpdatesDirMap);
        if (server.config.cacheUpdates) {
            try {
                writeCache(cacheFile);
            } catch (Throwable e) {
                logger.error("Write updates cache failed", e);
            }
        }
        server.modulesManager.invokeEvent(new LaunchServerUpdatesSyncEvent(server));
    }

    public HashSet<String> getUpdatesList() {
        HashSet<String> set = new HashSet<>();
        for (Map.Entry<String, HashedDir> entry : updatesDirMap.entrySet())
            set.add(entry.getKey());
        return set;
    }

    public HashedDir getUpdate(String name) {
        return updatesDirMap.get(name);
    }

    public void addUpdate(String name, HashedDir dir) {
        updatesDirMap.put(name, dir);
    }
}
