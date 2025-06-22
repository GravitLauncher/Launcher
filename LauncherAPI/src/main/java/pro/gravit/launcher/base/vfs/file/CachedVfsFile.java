package pro.gravit.launcher.base.vfs.file;

import pro.gravit.launcher.base.vfs.VfsFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;

public class CachedVfsFile extends VfsFile {
    private final VfsFile delegate;
    private volatile SoftReference<byte[]> cache;

    public CachedVfsFile(VfsFile delegate) {
        this.delegate = delegate;
    }

    private synchronized InputStream tryCache() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try(InputStream input = delegate.getInputStream()) {
                input.transferTo(output);
            }
            byte[] bytes = output.toByteArray();
            cache = new SoftReference<>(bytes);
            return new ByteArrayInputStream(bytes);
        } catch (OutOfMemoryError | IOException ignored) {
        }
        return null;
    }

    @Override
    public InputStream getInputStream() {
        var cachedBytes = cache == null ? null : cache.get();
        if(cachedBytes != null) {
            return new ByteArrayInputStream(cachedBytes);
        }
        var cached = tryCache();
        if(cached != null) {
            return cached;
        }
        return delegate.getInputStream();
    }
}
