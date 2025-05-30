package pro.gravit.launchserver.auth.updates;

import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.launcher.core.hasher.HashedEntry;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class UpdatesProvider {
    public static final ProviderMap<UpdatesProvider> providers = new ProviderMap<>("UpdatesProvider");
    private static boolean registredProviders = false;
    protected transient LaunchServer server;

    public static void registerProviders() {
        if (!registredProviders) {
            providers.register("local", LocalUpdatesProvider.class);
            registredProviders = true;
        }
    }

    public void init(LaunchServer server) {
        this.server = server;
    }

    public void sync() throws IOException {
        sync(null);
    }

    public abstract void syncInitially() throws IOException;

    public abstract void sync(Collection<String> updateNames) throws IOException;

    public abstract HashedDir getUpdatesDir(String updateName);

    public abstract void upload(String updateName, Map<String, Path> files, boolean deleteAfterUpload) throws IOException;

    public abstract OutputStream upload(String updateName, String file) throws IOException;

    public void upload(String updateName, Path dir, boolean deleteAfterUpload) throws IOException {
        if(!Files.isDirectory(dir)) {
            throw new UnsupportedEncodingException(String.format("%s is not a directory", dir));
        }
        Map<String, Path> map = new HashMap<>();
        IOHelper.walk(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                map.put(dir.relativize(file).toString(), file);
                return FileVisitResult.CONTINUE;
            }
        }, true);
        upload(updateName, map, deleteAfterUpload);
    }

    public abstract Map<String, Path> download(String updateName, List<String> files) throws IOException;

    public abstract void download(String updateName, Map<String, Path> files) throws IOException;

    public abstract InputStream download(String updateName, String path) throws IOException;

    public abstract void move(Map<UpdateNameAndFile, UpdateNameAndFile> files) throws IOException;

    public void move(String updateName, String newUpdateName) throws IOException {
        create(newUpdateName);
        var updatesDir = getUpdatesDir(updateName);
        Map<UpdateNameAndFile, UpdateNameAndFile> map = new HashMap<>();
        updatesDir.walk("/", (path, name, entry) -> {
            map.put(UpdateNameAndFile.of(updateName, path), UpdateNameAndFile.of(newUpdateName, path));
            return HashedDir.WalkAction.CONTINUE;
        });
        move(map);
        delete(updateName);
    }

    public abstract void copy(Map<UpdateNameAndFile, UpdateNameAndFile> files) throws IOException;

    public void copy(String updateName, String newUpdateName) throws IOException {
        create(newUpdateName);
        var updatesDir = getUpdatesDir(updateName);
        Map<UpdateNameAndFile, UpdateNameAndFile> map = new HashMap<>();
        updatesDir.walk("/", (path, name, entry) -> {
            map.put(UpdateNameAndFile.of(updateName, path), UpdateNameAndFile.of(newUpdateName, path));
            return HashedDir.WalkAction.CONTINUE;
        });
        copy(map);
    }

    public abstract void delete(String updateName, List<String> files) throws IOException;

    public abstract void delete(String updateName) throws IOException;

    public abstract void create(String updateName) throws IOException;

    public void close() {

    }

    public record UpdateNameAndFile(String updateName, String path) {
        public static UpdateNameAndFile of(String updateName, String path) {
            return new UpdateNameAndFile(updateName, path);
        }
    }
}
