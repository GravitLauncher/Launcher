package pro.gravit.utils.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedPathTest {
    @TempDir
    Path temporary;

    @Test
    void rejectsParentTraversalEvenWhenNormalizedPathStaysInside() {
        assertThrows(IOException.class, () -> ManagedPath.resolve(temporary, "child/../file"));
        assertThrows(IOException.class, () -> ManagedPath.resolve(temporary, "../outside"));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void rejectsSymlinkedManagedPathComponents() throws IOException {
        Path outside = Files.createDirectory(temporary.resolve("outside"));
        Path link = temporary.resolve("link");
        Files.createSymbolicLink(link, outside);
        assertThrows(IOException.class, () -> ManagedPath.resolve(temporary, "link/file"));
    }
}
