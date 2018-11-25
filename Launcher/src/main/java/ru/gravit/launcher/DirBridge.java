package ru.gravit.launcher;

import ru.gravit.launcher.LauncherAPI;

import java.nio.file.Path;

public class DirBridge {
    @LauncherAPI
    public static Path dir;
    @LauncherAPI
    public static Path dirUpdates;
    @LauncherAPI
    public static Path defaultUpdatesDir;

}
