package pro.gravit.launcher.base.vfs;


import pro.gravit.launcher.base.vfs.protocol.vfs.VfsURLConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public abstract class VfsFile extends VfsEntry {
    public URLConnection openConnection(URL url) {
        return new VfsURLConnection(url, this);
    }
    public abstract InputStream getInputStream();

    public byte[] readAll() throws IOException {
        try(var input = getInputStream()) {
            try(var output = new ByteArrayOutputStream()) {
                input.transferTo(output);
                return output.toByteArray();
            }
        }
    }
}
