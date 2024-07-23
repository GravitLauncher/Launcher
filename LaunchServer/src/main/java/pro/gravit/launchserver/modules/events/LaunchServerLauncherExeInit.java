package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.LauncherBinary;

public class LaunchServerLauncherExeInit extends LauncherModule.Event {
    public final LaunchServer server;
    public LauncherBinary binary;

    public LaunchServerLauncherExeInit(LaunchServer server, LauncherBinary binary) {
        this.server = server;
        this.binary = binary;
    }
}
