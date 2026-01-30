package pro.gravit.launcher.base.profiles.optional.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.utils.helper.LogHelper;

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
            HashedDir.FindRecursiveResult firstPath = dir.findRecursive(k);
            if (v != null && !v.isEmpty()) {
                logger.info("Debug findRecursive: name {}, parent: ", firstPath.name, firstPath.parent == null ? "null" : "not null", firstPath.entry == null ? "null" : "not null");
                HashedDir.FindRecursiveResult secondPath = dir.findRecursive(v);
                logger.info("Debug findRecursive: name {}, parent: ", secondPath.name, secondPath.parent == null ? "null" : "not null", secondPath.entry == null ? "null" : "not null");
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