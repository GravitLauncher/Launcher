package pro.gravit.launcher.base.vfs;


import pro.gravit.launcher.base.vfs.directory.SimpleVfsDirectory;
import pro.gravit.launcher.base.vfs.protocol.vfs.VfsURLStreamHandlerProvider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Vfs {
    private final VfsDirectory directory;
    private static final Map<String, Vfs> map = new HashMap<>();
    private static final Vfs defaultImpl = new Vfs();
    static {
        URL.setURLStreamHandlerFactory(new VfsURLStreamHandlerProvider());
        register("", defaultImpl);
    }

    public Vfs() {
        directory = new SimpleVfsDirectory();
    }

    public Vfs(VfsDirectory directory) {
        this.directory = directory;
    }

    public VfsEntry resolve(Path path) {
        return directory.resolve(path);
    }

    public void put(Path path, VfsEntry entry) {
        VfsEntry parent = resolve(path.getParent());
        if(parent instanceof SimpleVfsDirectory dir) {
            dir.put(path.getFileName().toString(), entry);
        } else {
            throw new VfsException(String.format("%s not support add new files", path.getParent()));
        }
    }

    public static void register(String name, Vfs vfs) {
        map.put(name, vfs);
    }

    public static Vfs getByName(String name) {
        if(name == null) {
            return defaultImpl;
        }
        return map.get(name);
    }

    public static Vfs get() {
        return defaultImpl;
    }

    public InputStream getInputStream(Path path) throws IOException {
        VfsEntry entry = directory.resolve(path);
        if (entry == null) throw new FileNotFoundException(String.format("File %s not found", path));
        if(entry instanceof VfsFile file) {
            return file.getInputStream();
        }
        throw new VfsException(String.format("%s is a directory", path.getParent()));
    }

    public URL getURL(String path) throws IOException {
        return getURL(Paths.get(path));
    }

    public URL getURL(Path name) throws IOException {
        try (InputStream stream = defaultImpl.getInputStream(name)) {
            return new URI("vfs", null, "/"+name, null).toURL();
        } catch (UnsupportedOperationException ex) {
            throw new FileNotFoundException(name.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
