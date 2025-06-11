package pro.gravit.launcher.core.backend;

public class LauncherBackendAPIHolder {
    private static volatile LauncherBackendAPI api;

    public static LauncherBackendAPI getApi() {
        return api;
    }

    public static void setApi(LauncherBackendAPI api) {
        LauncherBackendAPIHolder.api = api;
    }
}
