package pro.gravit.launcher;

import pro.gravit.launcher.client.UserSettings;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;

import java.nio.file.Path;
import java.util.*;

public class NewLauncherSettings {
    @LauncherAPI
    public String login;
    @LauncherAPI
    public String auth;
    @LauncherAPI
    public byte[] rsaPassword;
    @LauncherAPI
    public int profile;
    @LauncherAPI
    public transient Path updatesDir;
    @LauncherAPI
    public String updatesDirPath;
    @LauncherAPI
    public boolean autoEnter;
    @LauncherAPI
    public boolean debug;
    @LauncherAPI
    public boolean fullScreen;
    @LauncherAPI
    public boolean offline;
    @LauncherAPI
    public int ram;

    @LauncherAPI
    public byte[] lastDigest;
    @LauncherAPI
    public List<ClientProfile> lastProfiles = new LinkedList<>();
    @LauncherAPI
    public Map<String, UserSettings> userSettings = new HashMap<>();
    @LauncherAPI
    public boolean featureStore;
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
