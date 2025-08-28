package pro.gravit.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pro.gravit.launcher.base.vfs.Vfs;
import pro.gravit.launcher.base.vfs.VfsDirectory;
import pro.gravit.launcher.base.vfs.VfsEntry;
import pro.gravit.launcher.base.vfs.directory.OverlayVfsDirectory;
import pro.gravit.launcher.base.vfs.file.UrlVfsFile;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VfsTest {
    @Test
    public void testVfs() throws Exception {
        List<Path> paths = List.of(
                Path.of("test/testfile.txt"),
                Path.of("test/dir/testfile2.txt")
        );
        Map<Path, VfsEntry> testPaths  = new HashMap<>();
        for(var path : paths) {
            var file = new UrlVfsFile(URI.create("https://example.com").toURL());
            Vfs.get().put(path, file);
            testPaths.put(path, file);
        }
        for(var path : testPaths.entrySet()) {
            Assertions.assertSame(Vfs.get().resolve(path.getKey()), path.getValue());
        }
    }

    @Test
    public void testVfsWithOverlay() throws Exception {
        List<Path> paths = List.of(
                Path.of("test/testfile.txt"),
                Path.of("test/dir/testfile2.txt")
        );
        Path prefix = Path.of("base");
        Map<Path, VfsEntry> testPaths  = new HashMap<>();
        for(var path : paths) {
            var file = new UrlVfsFile(URI.create("https://example.com").toURL());
            Vfs.get().put(prefix.resolve(path), file);
            testPaths.put(path, file);
        }
        Path overlayPrefix = Path.of("oveerlay");
        OverlayVfsDirectory vfsDirectory = new OverlayVfsDirectory(List.of((VfsDirectory) Vfs.get().resolve(prefix)));
        Vfs.get().put(overlayPrefix, vfsDirectory);
        for(var path : testPaths.entrySet()) {
            Assertions.assertSame(Vfs.get().resolve(overlayPrefix.resolve(path.getKey())), path.getValue());
        }
    }
}
