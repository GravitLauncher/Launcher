package pro.gravit.launcher.base.vfs.protocol.vfs;


import pro.gravit.launcher.base.vfs.Vfs;
import pro.gravit.launcher.base.vfs.VfsFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        Vfs enFS = Vfs.getByName(url.getHost());
        String realPath = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
        var fileEntry = enFS.resolve(Paths.get(realPath));
        if(fileEntry == null) throw new FileNotFoundException(url.toString());
        if(fileEntry instanceof VfsFile file) {
            return file.openConnection(url);
        }
        throw new UnsupportedOperationException(String.format("%s not supported openConnection()", url));
    }
}
