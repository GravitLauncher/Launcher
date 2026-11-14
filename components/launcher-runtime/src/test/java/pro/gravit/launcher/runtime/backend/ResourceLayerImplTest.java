package pro.gravit.launcher.runtime.backend;

import org.junit.jupiter.api.Test;
import pro.gravit.launcher.base.vfs.Vfs;
import pro.gravit.launcher.base.vfs.file.BytesVfsFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceLayerImplTest {
    @Test
    void closeReleasesOverlayMountAndIsIdempotent() throws IOException {
        Path base = Path.of("resource-layer-test-" + UUID.randomUUID());
        Vfs.get().put(base.resolve("overlay/file"), new BytesVfsFile(new byte[]{1}));
        ResourceLayerImpl layer = new ResourceLayerImpl(base, List.of(Path.of("overlay")));
        layer.getURL(Path.of("file"));

        layer.close();
        layer.close();

        assertThrows(IOException.class, () -> layer.getURL(Path.of("file")));
        Vfs.get().remove(base);
    }
}
