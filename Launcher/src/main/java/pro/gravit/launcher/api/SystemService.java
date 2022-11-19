package pro.gravit.launcher.api;

import pro.gravit.launcher.LauncherEngine;

public class SystemService {
    private SystemService() {
        throw new UnsupportedOperationException();
    }

    public static void exit(int code) {
        LauncherEngine.exitLauncher(code);
    }
}
