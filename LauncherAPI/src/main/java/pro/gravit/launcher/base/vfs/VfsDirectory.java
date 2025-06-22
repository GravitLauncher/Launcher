package pro.gravit.launcher.base.vfs;

import java.nio.file.Path;
import java.util.stream.Stream;

public abstract class VfsDirectory extends VfsEntry {
    public abstract VfsEntry find(String name);
    public VfsEntry resolve(Path path) {
        if(path == null) {
            return null;
        }

        VfsDirectory current = this;
        for(int i=0;i<path.getNameCount();++i) {
            String s = path.getName(i).toString();
            VfsEntry entity = current.find(s);
            if(entity instanceof VfsDirectory newDir) {
                current = newDir;
            } else {
                return entity;
            }
        }
        return null;
    }
    protected abstract Stream<String> getFiles();
}
