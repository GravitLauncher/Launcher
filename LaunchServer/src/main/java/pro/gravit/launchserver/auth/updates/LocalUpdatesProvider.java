package pro.gravit.launchserver.auth.updates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.serialize.HInput;
import pro.gravit.launcher.core.serialize.HOutput;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerUpdatesSyncEvent;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

public class LocalUpdatesProvider extends UpdatesProvider {
    private final transient Logger logger = LogManager.getLogger();
    public String cacheFile = ".updates-cache";
    public String updatesDir = "updates";
    public boolean cacheUpdates = true;
    private volatile transient Map<String, HashedDir> updatesDirMap;

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

    public void readUpdatesFromCache() throws IOException {
        readCache(Path.of(cacheFile));
    }

    public void readUpdatesDir() throws IOException {
        var cacheFilePath = Path.of(cacheFile);
        if (cacheUpdates) {
            if (Files.exists(cacheFilePath)) {
                try {
                    readCache(cacheFilePath);
                    return;
                } catch (Throwable e) {
                    logger.error("Read updates cache failed", e);
                }
            }
        }
        sync(null);
    }

    @Override
    public void init(LaunchServer server) {
        super.init(server);
        try {
            if (!IOHelper.isDir(Path.of(updatesDir)))
                Files.createDirectory(Path.of(updatesDir));
        } catch (IOException e) {
            logger.error("Updates not synced", e);
        }
    }

    @Override
    public void syncInitially() throws IOException {
        readUpdatesDir();
    }

    public void sync(Collection<String> dirs) throws IOException {
        logger.info("Syncing updates dir");
        Map<String, HashedDir> newUpdatesDirMap = new HashMap<>(16);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Path.of(updatesDir))) {
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
        if (cacheUpdates) {
            try {
                writeCache(Path.of(cacheFile));
            } catch (Throwable e) {
                logger.error("Write updates cache failed", e);
            }
        }
        server.modulesManager.invokeEvent(new LaunchServerUpdatesSyncEvent(server));
    }

    @Override
    public HashedDir getUpdatesDir(String updateName) {
        return updatesDirMap.get(updateName);
    }

    private Path resolveUpdateName(String updateName) {
        if(updateName == null) {
            return Path.of(updatesDir);
        }
        return Path.of(updatesDir).resolve(updateName);
    }

    @Override
    public void upload(String updateName, Map<String, Path> files, boolean deleteAfterUpload) throws IOException {
        var path = resolveUpdateName(updateName);
        for(var e : files.entrySet()) {
            var target = path.resolve(e.getKey());
            var source = e.getValue();
            IOHelper.createParentDirs(target);
            if(deleteAfterUpload) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Override
    public Map<String, Path> download(String updateName, List<String> files) {
        var path = resolveUpdateName(updateName);
        Map<String, Path> map = new HashMap<>();
        for(var e : files) {
            map.put(e, path.resolve(e));
        }
        return map;
    }

    @Override
    public void delete(String updateName, List<String> files) throws IOException {
        var path = resolveUpdateName(updateName);
        for(var e : files) {
            var target = path.resolve(e);
            Files.delete(target);
        }
    }

    @Override
    public void delete(String updateName) throws IOException {
        var path = resolveUpdateName(updateName);
        IOHelper.deleteDir(path, true);
    }

    @Override
    public void create(String updateName) throws IOException {
        var path = resolveUpdateName(updateName);
        Files.createDirectories(path);
    }
}
