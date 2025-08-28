package pro.gravit.launcher.base.vfs.protocol.vfs;


import pro.gravit.launcher.base.vfs.VfsFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class VfsURLConnection extends URLConnection {
    private final VfsFile fileEntry;
    public VfsURLConnection(URL url, VfsFile fileEntry) {
        super(url);
        this.fileEntry = fileEntry;
    }

    @Override
    public void connect() throws IOException {
        // NOP
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return fileEntry.getInputStream();
    }
}
