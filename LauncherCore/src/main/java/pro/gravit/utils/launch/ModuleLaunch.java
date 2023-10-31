package pro.gravit.utils.launch;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class ModuleLaunch implements Launch {
    @Override
    public ClassLoaderControl init(List<Path> files, String nativePath, LaunchOptions options) {
        throw new UnsupportedOperationException("Please use Multi-Release JAR");
    }

    @Override
    public void launch(String mainClass, String mainModule, Collection<String> args) throws Throwable {
        throw new UnsupportedOperationException("Please use Multi-Release JAR");
    }
}
