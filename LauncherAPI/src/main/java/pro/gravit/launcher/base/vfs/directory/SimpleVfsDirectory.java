package pro.gravit.launcher.base.vfs.directory;


import pro.gravit.launcher.base.vfs.VfsDirectory;
import pro.gravit.launcher.base.vfs.VfsEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class SimpleVfsDirectory extends VfsDirectory {
    private final Map<String, VfsEntry> map = new HashMap<>();
    @Override
    public VfsEntry find(String name) {
        return map.get(name);
    }

    public VfsEntry remove(String name) {
        return map.remove(name);
    }

    public void put(String name, VfsEntry entry) {
        map.put(name, entry);
    }

    @Override
    protected Stream<String> getFiles() {
        return map.keySet().stream();
    }
}
