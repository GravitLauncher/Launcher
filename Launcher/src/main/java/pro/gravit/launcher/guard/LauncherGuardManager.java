package pro.gravit.launcher.guard;

import java.nio.file.Path;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;

public class LauncherGuardManager {
    public static LauncherGuardInterface guard;

    public static void initGuard(boolean clientInstance) {
        LauncherConfig config = Launcher.getConfig();
        switch (config.guardType) {
            case "gravitguard": {
                guard = new LauncherGravitGuard();
                break;
            }
            case "wrapper": {
                guard = new LauncherWrapperGuard();
                break;
            }
            case "java": {
                guard = new LauncherJavaGuard();
                break;
            }
            default: {
                guard = new LauncherNoGuard();
            }
        }
        guard.init(clientInstance);
    }

    public static Path getGuardJavaBinPath() {
        return guard.getJavaBinPath();
    }
}
