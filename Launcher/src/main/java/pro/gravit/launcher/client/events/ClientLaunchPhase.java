package pro.gravit.launcher.client.events;

import pro.gravit.launcher.client.ClientLauncher;
import pro.gravit.launcher.modules.LauncherModule;

public class ClientLaunchPhase extends LauncherModule.Event {
    public final ClientLauncher.Params params;

    public ClientLaunchPhase(ClientLauncher.Params params) {
        this.params = params;
    }
}
