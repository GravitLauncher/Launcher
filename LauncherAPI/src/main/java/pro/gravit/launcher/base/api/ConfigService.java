package pro.gravit.launcher.base.api;

public class ConfigService {
    public static boolean disableLogging;
    public static String serverName;
    public static CheckServerConfig checkServerConfig = new CheckServerConfig();
    public static class CheckServerConfig {
        public boolean needProperties;
        public boolean needHardware;
    }
}
