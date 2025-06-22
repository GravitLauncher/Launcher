package pro.gravit.launcher.base.vfs.directory;

import pro.gravit.launcher.base.vfs.VfsDirectory;
import pro.gravit.launcher.base.vfs.VfsEntry;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class OverlayVfsDirectory extends VfsDirectory {
    private final List<VfsDirectory> list;

    public OverlayVfsDirectory(List<VfsDirectory> list) {
        this.list = list;
    }

    @Override
    public VfsEntry find(String name) {
        for(var e : list) {
            var result = e.find(name);
            if(result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public VfsEntry resolve(Path path) {
        for(var e : list) {
            var result = e.resolve(path);
            if(result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public Stream<String> getFiles() {
        Stream<String> stream = Stream.empty();
        for(var e : list) {
            var result = e.getFiles();
            if(result != null) {
                stream = Stream.concat(stream, result);
            }
        }
        return stream;
    }
}
