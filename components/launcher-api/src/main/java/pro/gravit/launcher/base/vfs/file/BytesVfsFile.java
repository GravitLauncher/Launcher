package pro.gravit.launcher.base.vfs.file;


import pro.gravit.launcher.base.vfs.VfsFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class BytesVfsFile extends VfsFile {
    private final byte[] bytes;

    public BytesVfsFile(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }
}
