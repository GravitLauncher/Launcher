package pro.gravit.utils.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves untrusted relative names beneath a managed directory without following symlinks. */
public final class ManagedPath {
    private ManagedPath() { }

    public static Path resolve(Path root, String relative) throws IOException {
        if (relative == null) throw new IOException("Managed path must not be null");
        Path child = Path.of(relative);
        if (child.isAbsolute()) throw new IOException("Absolute managed paths are not allowed: " + relative);
        for (Path part : child) {
            if (part.toString().equals("..")) throw new IOException("Parent traversal is not allowed in managed paths: " + relative);
        }
        Path base = root.toAbsolutePath().normalize();
        Path result = base.resolve(child).normalize();
        if (!result.startsWith(base)) throw new IOException("Managed path escapes its root: " + relative);
        // The caller chooses and therefore trusts the root itself and its ancestors. Only
        // reject links at the root entry and beneath it, where names cross the trust boundary.
        Path current = base;
        if (Files.isSymbolicLink(current)) throw new IOException("Managed root must not be a symbolic link: " + current);
        for (Path part : base.relativize(result)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) throw new IOException("Symbolic links are not allowed in managed paths: " + current);
        }
        return result;
    }
}
