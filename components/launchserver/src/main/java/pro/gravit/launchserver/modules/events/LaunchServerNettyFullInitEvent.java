package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launchserver.LaunchServer;

public class LaunchServerNettyFullInitEvent extends LauncherModule.Event {
    public final LaunchServer server;

    public LaunchServerNettyFullInitEvent(LaunchServer server) {
        this.server = server;
    }
}
