package pro.gravit.launcher.base.vfs.directory;


import pro.gravit.launcher.base.vfs.VfsDirectory;
import pro.gravit.launcher.base.vfs.VfsEntry;
import pro.gravit.launcher.base.vfs.VfsException;
import pro.gravit.launcher.base.vfs.file.FileVfsFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileVfsDirectory extends VfsDirectory {
    private final Path path;

    public FileVfsDirectory(Path path) {
        this.path = path;
    }

    @Override
    public VfsEntry find(String name) {
        Path target = path.resolve(name);
        if(Files.exists(target)) {
            if(Files.isDirectory(target)) {
                return new FileVfsDirectory(target);
            }
            return new FileVfsFile(target);
        }
        return null;
    }

    @Override
    public VfsEntry resolve(Path path) {
        if(path == null) {
            return null;
        }

        Path target = path.resolve(path);
        if(Files.exists(target)) {
            if(Files.isDirectory(target)) {
                return new FileVfsDirectory(target);
            }
            return new FileVfsFile(target);
        }
        return null;
    }

    @SuppressWarnings("resource")
    @Override
    public Stream<String> getFiles() {
        try {
            return Files.list(path).map(Path::getFileName).map(Path::toString);
        } catch (IOException e) {
            throw new VfsException(e);
        }
    }
}
