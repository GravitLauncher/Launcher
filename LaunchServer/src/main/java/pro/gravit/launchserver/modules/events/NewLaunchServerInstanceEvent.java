package pro.gravit.launchserver.modules.events;

import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launchserver.LaunchServer;

public class NewLaunchServerInstanceEvent extends LauncherModule.Event {
    public final LaunchServer launchServer;

    public NewLaunchServerInstanceEvent(LaunchServer launchServer) {
        this.launchServer = launchServer;
    }
}
