package pro.gravit.launcher.client.api;

import pro.gravit.launcher.client.ClientLauncherMethods;

public class SystemService {
    private SystemService() {
        throw new UnsupportedOperationException();
    }

    public static void exit(int code) {
        ClientLauncherMethods.exitLauncher(code);
    }
}
