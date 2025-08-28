package pro.gravit.launcher.base.vfs.file;


import pro.gravit.launcher.base.vfs.VfsFile;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class DelegateVfsFile extends VfsFile {
    private final VfsFile vfsFile;

    public DelegateVfsFile(VfsFile vfsFile) {
        this.vfsFile = vfsFile;
    }

    @Override
    public URLConnection openConnection(URL url) {
        return vfsFile.openConnection(url);
    }

    @Override
    public InputStream getInputStream() {
        return vfsFile.getInputStream();
    }
}
