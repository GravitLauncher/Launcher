package pro.gravit.launcher.runtime.debug;

import pro.gravit.launcher.base.LauncherConfig;

public class DebugProperties {
    public static final String ACCESS_TOKEN = System.getProperty("launcher.runtime.auth.accesstoken", null);
    public static final String AUTH_ID = System.getProperty("launcher.runtime.auth.authid", "std");
    public static final String REFRESH_TOKEN = System.getProperty("launcher.runtime.auth.refreshtoken", null);
    public static final String MINECRAFT_ACCESS_TOKEN = System.getProperty("launcher.runtime.auth.minecraftaccesstoken", "DEBUG");
    public static final long EXPIRE = Long.parseLong(System.getProperty("launcher.runtime.auth.expire", "0"));
    public static final String USERNAME = System.getProperty("launcher.runtime.username", null);
    public static final String LOGIN = System.getProperty("launcher.runtime.login", USERNAME);
    public static final String UUID = System.getProperty("launcher.runtime.uuid", null);
    public static final String PASSWORD = System.getProperty("launcher.runtime.password", null);
    public static String WEBSOCKET_URL = System.getProperty("launcherdebug.websocket", "ws://localhost:9274/api");
    public static String PROJECT_NAME = System.getProperty("launcherdebug.projectname", "Minecraft");
    public static String UNLOCK_SECRET = System.getProperty("launcherdebug.unlocksecret", "");
    public static boolean DISABLE_CONSOLE = Boolean.getBoolean("launcherdebug.disableConsole");
    public static boolean OFFLINE_MODE = Boolean.getBoolean("launcherdebug.offlinemode");
    public static boolean DISABLE_AUTO_REFRESH = Boolean.getBoolean("launcherdebug.disableautorefresh");
    public static String[] MODULE_CLASSES = System.getProperty("launcherdebug.modules", "").split(",");
    public static String[] MODULE_FILES = System.getProperty("launcherdebug.modulefiles", "").split(",");
    public static LauncherConfig.LauncherEnvironment ENV = LauncherConfig.LauncherEnvironment.valueOf(System.getProperty("launcherdebug.env", "STD"));
}
