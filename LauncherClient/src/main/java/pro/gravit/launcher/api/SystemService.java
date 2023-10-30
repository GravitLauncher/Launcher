package pro.gravit.launcher.api;

import pro.gravit.launcher.ClientLauncherMethods;

public class SystemService {
    private SystemService() {
        throw new UnsupportedOperationException();
    }

    public static void exit(int code) {
        ClientLauncherMethods.exitLauncher(code);
    }
}
