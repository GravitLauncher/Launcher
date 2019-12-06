package pro.gravit.launcher;

import pro.gravit.launcher.client.UserSettings;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;

import java.nio.file.Path;
import java.util.*;

public class NewLauncherSettings {
    @LauncherAPI
    public Map<String, UserSettings> userSettings = new HashMap<>();
    @LauncherAPI
    public List<String> features = new ArrayList<>();
    @LauncherAPI
    public String consoleUnlockKey;

    public static class HashedStoreEntry {
        @LauncherAPI
        public final HashedDir hdir;
        @LauncherAPI
        public final String name;
        @LauncherAPI
        public final String fullPath;
        @LauncherAPI
        public transient boolean needSave = false;

        public HashedStoreEntry(HashedDir hdir, String name, String fullPath) {
            this.hdir = hdir;
            this.name = name;
            this.fullPath = fullPath;
        }
    }

    @LauncherAPI
    public final transient List<HashedStoreEntry> lastHDirs = new ArrayList<>(16);

    @LauncherAPI
    public void putHDir(String name, Path path, HashedDir dir) {
        String fullPath = path.toAbsolutePath().toString();
        lastHDirs.removeIf((e) -> e.fullPath.equals(fullPath) && e.name.equals(name));
        HashedStoreEntry e = new HashedStoreEntry(dir, name, fullPath);
        e.needSave = true;
        lastHDirs.add(e);
    }
}
