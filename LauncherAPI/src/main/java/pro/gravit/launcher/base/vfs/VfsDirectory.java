package pro.gravit.launcher.base.vfs;

import java.nio.file.Path;
import java.util.stream.Stream;

public abstract class VfsDirectory extends VfsEntry {
    public abstract VfsEntry find(String name);
    public abstract VfsEntry resolve(Path path);
    public abstract Stream<String> getFiles();
}
