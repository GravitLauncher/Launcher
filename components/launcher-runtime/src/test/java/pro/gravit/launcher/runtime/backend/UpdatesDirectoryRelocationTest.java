package pro.gravit.launcher.runtime.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UpdatesDirectoryRelocationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void copiesToTargetAndRetainsSourceUntilCommit() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        Files.createDirectories(source.resolve("runtime/bin"));
        Files.writeString(source.resolve("runtime/bin/java"), "runtime");
        Path target = temporaryDirectory.resolve("destination/updates");

        LauncherBackendImpl.copyUpdatesDirectory(source, target);

        assertEquals("runtime", Files.readString(target.resolve("runtime/bin/java")));
        assertEquals("runtime", Files.readString(source.resolve("runtime/bin/java")));
    }

    @Test
    void refusesNonEmptyTargetWithoutChangingSource() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        Files.writeString(source.resolve("keep.txt"), "source");
        Path target = Files.createDirectory(temporaryDirectory.resolve("target"));
        Files.writeString(target.resolve("keep.txt"), "target");

        assertThrows(IOException.class, () -> LauncherBackendImpl.copyUpdatesDirectory(source, target));
        assertEquals("source", Files.readString(source.resolve("keep.txt")));
        assertEquals("target", Files.readString(target.resolve("keep.txt")));
    }

    @Test
    void rejectsNestedPaths() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        assertThrows(IllegalArgumentException.class,
                () -> LauncherBackendImpl.copyUpdatesDirectory(source, source.resolve("child")));
    }
}
