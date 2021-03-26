package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launchserver.LaunchServer;

public class LaunchServerProfilesSyncEvent extends LauncherModule.Event {
    public final LaunchServer server;

    public LaunchServerProfilesSyncEvent(LaunchServer server) {
        this.server = server;
    }
}
