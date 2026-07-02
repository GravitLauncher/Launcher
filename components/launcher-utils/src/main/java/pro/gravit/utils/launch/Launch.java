package pro.gravit.utils.launch;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public interface Launch {
    ClassLoaderControl init(List<Path> files, String nativePath, LaunchOptions options);
    void launch(String mainClass, String mainModule, Collection<String> args) throws Throwable;
}
