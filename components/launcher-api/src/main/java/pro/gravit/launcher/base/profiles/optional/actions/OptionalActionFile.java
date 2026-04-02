package pro.gravit.launcher.base.profiles.optional.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.core.hasher.HashedDir;

import java.util.Map;

public class OptionalActionFile extends OptionalAction {

    private static final Logger logger =
            LoggerFactory.getLogger(OptionalActionFile.class);

    public Map<String, String> files;

    public OptionalActionFile() {
    }

    public OptionalActionFile(Map<String, String> files) {
        this.files = files;
    }

    public void injectToHashedDir(HashedDir dir) {
        if (files == null) return;
        files.forEach((k, v) -> {
            if (v == null || v.isEmpty()) return;
            HashedDir.FindRecursiveResult source = dir.tryFindRecursive(k);
            if (!source.isFound()) {
                logger.warn("OptionalActionFile source not found for move: {}", k);
                return;
            }
            HashedDir.FindRecursiveResult target = dir.createParentDirectories(v);
            source.parent.moveTo(source.name, target.parent, target.name);
        });
    }

    public void disableInHashedDir(HashedDir dir) {
        if (files == null) return;
        files.forEach((k, v) -> {
            HashedDir.FindRecursiveResult result = dir.tryFindRecursive(k);
            if (result.isFound()) {
                result.parent.remove(result.name);
            }
        });
    }
}
