package pro.gravit.launcher.base.vfs.file;


import pro.gravit.launcher.base.vfs.VfsFile;

import java.io.InputStream;
import java.util.function.Supplier;

public class InputStreamVfsFile extends VfsFile {
    private final Supplier<InputStream> streamSupplier;

    public InputStreamVfsFile(Supplier<InputStream> streamSupplier) {
        this.streamSupplier = streamSupplier;
    }

    @Override
    public InputStream getInputStream() {
        return streamSupplier.get();
    }
}
