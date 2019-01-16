package ru.gravit.launcher.client;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JVMHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirBridge {
    @LauncherAPI
    public static Path dir;
    @LauncherAPI
    public static Path dirUpdates;
    @LauncherAPI
    public static Path defaultUpdatesDir;
    @LauncherAPI
    public static boolean useLegacyDir;

    @LauncherAPI
    public static void move(Path newDir) throws IOException {
        IOHelper.move(dirUpdates, newDir);
        dirUpdates = newDir;
    }

    @LauncherAPI
    public static Path getAppDataDir() throws IOException {
        Boolean isCustomDir = Boolean.getBoolean(System.getProperty("launcher.usecustomdir", "false"));
        if (isCustomDir) {
            return Paths.get(System.getProperty("launcher.customdir"));
        }
        if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            Boolean isOpt = Boolean.getBoolean(System.getProperty("launcher.useoptdir", "false"));
            if (isOpt) {
                Path opt = Paths.get("/").resolve("opt");
                if (!IOHelper.isDir(opt)) Files.createDirectories(opt);
                return opt;
            } else {
                Path local = IOHelper.HOME_DIR.resolve(".minecraftlauncher");
                if (!IOHelper.isDir(local)) Files.createDirectories(local);
                return local;
            }
        } else if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            Path appdata = IOHelper.HOME_DIR.resolve("AppData").resolve("Roaming");
            if (!IOHelper.isDir(appdata)) Files.createDirectories(appdata);
            return appdata;
        } else if (JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX) {
            Path minecraft = IOHelper.HOME_DIR.resolve("minecraft");
            if (!IOHelper.isDir(minecraft)) Files.createDirectories(minecraft);
            return minecraft;
        } else {
            return IOHelper.HOME_DIR;
        }
    }

    @LauncherAPI
    public static Path getLauncherDir(String projectname) throws IOException {
        return getAppDataDir().resolve(projectname);
    }

    @LauncherAPI
    public static Path getLegacyLauncherDir(String projectname) {
        return IOHelper.HOME_DIR.resolve(projectname);
    }

    @LauncherAPI
    public static void setUseLegacyDir(boolean b) {
        useLegacyDir = b;
    }
}
