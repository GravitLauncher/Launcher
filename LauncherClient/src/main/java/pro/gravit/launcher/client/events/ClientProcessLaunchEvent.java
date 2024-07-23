package pro.gravit.launcher.client.events;

import pro.gravit.launcher.client.ClientParams;
import pro.gravit.launcher.base.modules.LauncherModule;

public class ClientProcessLaunchEvent extends LauncherModule.Event {
    public final ClientParams params;

    public ClientProcessLaunchEvent(ClientParams params) {
        this.params = params;
    }
}
