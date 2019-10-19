package pro.gravit.launcher.guard;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;

import java.nio.file.Path;

public class LauncherGuardManager {
    public static LauncherGuardInterface guard;

    public static void initGuard(boolean clientInstance) {
        if(guard == null)
        {
            LauncherConfig config = Launcher.getConfig();
            switch (config.guardType) {
                case "stdguard": {
                    guard = new LauncherStdGuard();
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
        }
        guard.init(clientInstance);
    }

    public static Path getGuardJavaBinPath() {
        return guard.getJavaBinPath();
    }
}
