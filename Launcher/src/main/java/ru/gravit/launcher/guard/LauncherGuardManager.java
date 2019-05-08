package ru.gravit.launcher.guard;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.LauncherConfig;

import java.nio.file.Path;

public class LauncherGuardManager {
    public static LauncherGuardInterface guard;

    public static void initGuard(boolean clientInstance) {
        LauncherConfig config = Launcher.getConfig();
        switch (config.guardType)
        {
            case "wrapper":
            {
                guard = new LauncherWrapperGuard();
				break;
            }
            case "java":
            {
                guard = new LauncherJavaGuard();
				break;
            }
            default:
            {
                guard = new LauncherNoGuard();
            }
        }
        guard.init(clientInstance);
    }

    public static Path getGuardJavaBinPath() {
        return guard.getJavaBinPath();
    }
}
