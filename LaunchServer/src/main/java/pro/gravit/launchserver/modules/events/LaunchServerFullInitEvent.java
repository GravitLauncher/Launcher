package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launchserver.LaunchServer;

public class LaunchServerFullInitEvent extends LauncherModule.Event {
    public final LaunchServer server;

    public LaunchServerFullInitEvent(LaunchServer server) {
        this.server = server;
    }
}
