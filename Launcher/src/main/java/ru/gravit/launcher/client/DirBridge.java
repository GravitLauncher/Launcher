package ru.gravit.launcher.client;

import java.io.IOException;
import java.nio.file.Path;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.IOHelper;

public class DirBridge {
    @LauncherAPI
    public static Path dir;
    @LauncherAPI
    public static Path dirUpdates;
    @LauncherAPI
    public static Path defaultUpdatesDir;
    @LauncherAPI
    public static void move(Path newDir) throws IOException
    {
        IOHelper.move(dirUpdates,newDir);
        dirUpdates = newDir;
    }
}
