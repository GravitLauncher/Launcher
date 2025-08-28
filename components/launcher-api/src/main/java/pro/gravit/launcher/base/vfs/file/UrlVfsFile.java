package pro.gravit.launcher.base.vfs.file;


import pro.gravit.launcher.base.vfs.VfsException;
import pro.gravit.launcher.base.vfs.VfsFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class UrlVfsFile extends VfsFile {
    private final URL internalUrl;

    public UrlVfsFile(URL internalUrl) {
        this.internalUrl = internalUrl;
    }

    @Override
    public URLConnection openConnection(URL url) {
        try {
            return internalUrl.openConnection();
        } catch (IOException e) {
            throw new VfsException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return openConnection(internalUrl).getInputStream();
        } catch (IOException e) {
            throw new VfsException(e);
        }
    }
}
