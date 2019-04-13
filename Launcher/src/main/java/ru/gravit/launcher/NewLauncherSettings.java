package ru.gravit.launcher;

import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.launcher.profiles.ClientProfile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NewLauncherSettings {
    @LauncherAPI
    public String login;
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
    public Map<String, HashedDir> lastHDirs = new HashMap<>(16);
}
