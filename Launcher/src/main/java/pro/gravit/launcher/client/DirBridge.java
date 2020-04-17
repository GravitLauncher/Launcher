package pro.gravit.launcher.client;

import pro.gravit.launcher.Launcher;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirBridge {

    public static final String USE_CUSTOMDIR_PROPERTY = "launcher.usecustomdir";
    public static final String CUSTOMDIR_PROPERTY = "launcher.customdir";
    public static final String USE_OPTDIR_PROPERTY = "launcher.useoptdir";


    public static Path dir;

    public static Path dirStore;

    public static Path dirProjectStore;

    public static Path dirUpdates;

    public static Path defaultUpdatesDir;

    public static boolean useLegacyDir;

    static {
        String projectName = Launcher.getConfig().projectName;
        try {
            DirBridge.dir = getLauncherDir(projectName);
            if (!IOHelper.exists(DirBridge.dir)) Files.createDirectories(DirBridge.dir);
            DirBridge.defaultUpdatesDir = DirBridge.dir.resolve("updates");
            if (!IOHelper.exists(DirBridge.defaultUpdatesDir)) Files.createDirectories(DirBridge.defaultUpdatesDir);
            DirBridge.dirStore = getStoreDir(projectName);
            if (!IOHelper.exists(DirBridge.dirStore)) Files.createDirectories(DirBridge.dirStore);
            DirBridge.dirProjectStore = getProjectStoreDir(projectName);
            if (!IOHelper.exists(DirBridge.dirProjectStore)) Files.createDirectories(DirBridge.dirProjectStore);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public static void move(Path newDir) throws IOException {
        if (newDir == null) {
            LogHelper.debug("Invalid dir (null)");
            if (LogHelper.isDevEnabled())
                LogHelper.dev(LogHelper.toString(new Throwable("Check stack of call DirBridge with null path...")));
            return;
        }
        Path oldUpdates = dirUpdates;
        dirUpdates = newDir;
        LogHelper.dev(newDir.toString());
        IOHelper.move(oldUpdates, dirUpdates);
    }

    public static Path getAppDataDir() throws IOException {
        boolean isCustomDir = Boolean.getBoolean(System.getProperty(USE_CUSTOMDIR_PROPERTY, "false"));
        if (isCustomDir) {
            return Paths.get(System.getProperty(CUSTOMDIR_PROPERTY));
        }
        if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            boolean isOpt = Boolean.getBoolean(System.getProperty(USE_OPTDIR_PROPERTY, "false"));
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
            if (System.getenv().containsKey("appdata"))
                return Paths.get(System.getenv().get("appdata")).toAbsolutePath();
            if (System.getenv().containsKey("APPDATA")) // Because it is windows
                return Paths.get(System.getenv().get("APPDATA")).toAbsolutePath();
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

    public static Path getLauncherDir(String projectname) throws IOException {
        return getAppDataDir().resolve(projectname);
    }

    public static Path getStoreDir(String projectname) throws IOException {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX)
            return getAppDataDir().resolve("store");
        else if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE)
            return getAppDataDir().resolve("GravitLauncherStore");
        else
            return getAppDataDir().resolve("minecraftStore");
    }

    public static Path getProjectStoreDir(String projectname) throws IOException {
        return getStoreDir(projectname).resolve(projectname);
    }

    public static Path getGuardDir() {
        return dir.resolve("guard");
    }

    public static Path getLegacyLauncherDir(String projectname) {
        return IOHelper.HOME_DIR.resolve(projectname);
    }

    public static void setUseLegacyDir(boolean b) {
        useLegacyDir = b;
    }
}
