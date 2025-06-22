package pro.gravit.launcher.base.vfs.protocol.vfs;

import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

public class VfsURLStreamHandlerProvider extends URLStreamHandlerProvider {
    @Override
    public URLStreamHandler createURLStreamHandler(String s) {
        if (s.equals("vfs")) {
            return new Handler();
        }
        return null;
    }
}
