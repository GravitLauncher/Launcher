package pro.gravit.launcher.profiles.optional.actions;

import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.utils.helper.LogHelper;

import java.util.Map;

public class OptionalActionFile extends OptionalAction {
    public Map<String, String> files;

    public OptionalActionFile() {
    }

    public OptionalActionFile(Map<String, String> files) {
        this.files = files;
    }

    public void injectToHashedDir(HashedDir dir) {
        if (files == null) return;
        files.forEach((k, v) -> {
            HashedDir.FindRecursiveResult firstPath = dir.findRecursive(k);
            if (v != null && !v.isEmpty()) {
                LogHelper.dev("Debug findRecursive: name %s, parent: ", firstPath.name, firstPath.parent == null ? "null" : "not null", firstPath.entry == null ? "null" : "not null");
                HashedDir.FindRecursiveResult secondPath = dir.findRecursive(v);
                LogHelper.dev("Debug findRecursive: name %s, parent: ", secondPath.name, secondPath.parent == null ? "null" : "not null", secondPath.entry == null ? "null" : "not null");
                firstPath.parent.moveTo(firstPath.name, secondPath.parent, secondPath.name);
            }
        });
    }

    public void disableInHashedDir(HashedDir dir) {
        if (files == null) return;
        files.forEach((k, v) -> {
            HashedDir.FindRecursiveResult firstPath = dir.findRecursive(k);
            firstPath.parent.remove(firstPath.name);
        });
    }
}
