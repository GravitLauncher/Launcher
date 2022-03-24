package pro.gravit.launcher.api;

import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.LogHelper;

public class SystemService {
    private SystemService() {
        throw new UnsupportedOperationException();
    }

    public static void exit(int code) {
        LauncherEngine.exitLauncher(code);
    }
}
