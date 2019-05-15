package ru.gravit.launcher;

import ru.gravit.launcher.client.UserSettings;
import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.profiles.ClientProfile;

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

    public static class HashedStoreEntry {
        @LauncherAPI
        public HashedDir hdir;
        @LauncherAPI
        public String name;
        @LauncherAPI
        public String fullPath;

        public HashedStoreEntry(HashedDir hdir, String name, String fullPath) {
            this.hdir = hdir;
            this.name = name;
            this.fullPath = fullPath;
        }
    }

    @LauncherAPI
    public transient List<HashedStoreEntry> lastHDirs = new ArrayList<>(16);

    @LauncherAPI
    public void putHDir(String name, Path path, HashedDir dir) {
        String fullPath = path.toAbsolutePath().toString();
        for (HashedStoreEntry e : lastHDirs) {
            if (e.fullPath.equals(fullPath) && e.name.equals(name)) return;
        }
        lastHDirs.add(new HashedStoreEntry(dir, name, fullPath));
    }
}
