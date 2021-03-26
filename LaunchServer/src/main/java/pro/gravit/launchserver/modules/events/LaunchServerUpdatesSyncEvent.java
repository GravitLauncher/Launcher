package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launchserver.LaunchServer;

public class LaunchServerUpdatesSyncEvent extends LauncherModule.Event {
    public final LaunchServer server;

    public LaunchServerUpdatesSyncEvent(LaunchServer server) {
        this.server = server;
    }
}
