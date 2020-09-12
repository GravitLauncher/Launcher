package pro.gravit.launcher.profiles.optional.actions;

import pro.gravit.launcher.hasher.HashedDir;

import java.util.List;
import java.util.Map;

public class OptionalActionFile extends OptionalAction {
    public Map<String, String> files;
    public void injectToHashedDir(HashedDir dir)
    {
        if(files == null) return;
        files.forEach((k,v) -> {
            HashedDir.FindRecursiveResult firstPath = dir.findRecursive(k);
            if (v != null && !v.isEmpty()) {
                HashedDir.FindRecursiveResult secondPath = dir.findRecursive(v);
                firstPath.parent.moveTo(firstPath.name, secondPath.parent, secondPath.name);
            }
        });
    }
    public void disableInHashedDir(HashedDir dir)
    {
        if(files == null) return;
        files.forEach((k,v) -> {
            HashedDir.FindRecursiveResult firstPath = dir.findRecursive(k);
            firstPath.parent.remove(firstPath.name);
        });
    }
}
