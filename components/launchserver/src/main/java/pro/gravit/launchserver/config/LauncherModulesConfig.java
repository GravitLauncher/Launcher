package pro.gravit.launchserver.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LauncherModulesConfig {
    public List<String> loadModules = new ArrayList<>();
    public List<String> loadLauncherModules = new ArrayList<>();

    public static Path resolveModule(Path modulesDir, String entry) {
        Path path = Path.of(entry);
        Path bundled = modulesDir.resolve(path);
        if (Files.isRegularFile(bundled) || Files.isDirectory(bundled)) return bundled;
        if (entry.endsWith(".jar") || Files.isDirectory(path)) return path;
        return modulesDir.resolve(entry + ".jar");
    }
}
