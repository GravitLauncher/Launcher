package pro.gravit.launcher.base.vfs.directory;


import pro.gravit.launcher.base.vfs.VfsDirectory;
import pro.gravit.launcher.base.vfs.VfsEntry;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class SimpleVfsDirectory extends VfsDirectory {
    private final Map<String, VfsEntry> map = new HashMap<>();
    @Override
    public VfsEntry find(String name) {
        return map.get(name);
    }

    @Override
    public VfsEntry resolve(Path path)  {
        if(path == null) {
            return this;
        }

        VfsDirectory current = this;
        for(int i=0;i<path.getNameCount();++i) {
            String s = path.getName(i).toString();
            VfsEntry entity = current.find(s);
            if(entity instanceof VfsDirectory newDir) {
                if(entity instanceof SimpleVfsDirectory) {
                    current = newDir;
                } else {
                    if (i+1 >= path.getNameCount()) {
                        return newDir;
                    }
                    Path newPath = path.subpath(i+1, path.getNameCount());
                    return newDir.resolve(newPath);
                }
            } else {
                return entity;
            }
        }
        return current;
    }

    public VfsEntry remove(String name) {
        return map.remove(name);
    }

    public void put(String name, VfsEntry entry) {
        map.put(name, entry);
    }

    @Override
    public Stream<String> getFiles() {
        return map.keySet().stream();
    }
}
