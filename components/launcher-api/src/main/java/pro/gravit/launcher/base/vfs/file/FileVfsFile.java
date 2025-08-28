package pro.gravit.launcher.base.vfs.file;


import pro.gravit.launcher.base.vfs.VfsException;
import pro.gravit.launcher.base.vfs.VfsFile;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

public class FileVfsFile extends VfsFile {
    private final File file;

    public FileVfsFile(File file) {
        this.file = file;
    }

    public FileVfsFile(Path path) {
        this.file = path.toFile();
    }

    @Override
    public URLConnection openConnection(URL url) {
        try {
            return file.toURI().toURL().openConnection();
        } catch (IOException e) {
            throw new VfsException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new VfsException(e);
        }
    }
}
