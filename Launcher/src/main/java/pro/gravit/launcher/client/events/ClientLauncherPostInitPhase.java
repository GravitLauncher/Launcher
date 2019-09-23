package pro.gravit.launcher.client.events;

import pro.gravit.launcher.client.ClientLauncher;
import pro.gravit.launcher.modules.events.PostInitPhase;

public class ClientLauncherPostInitPhase extends PostInitPhase {
    public final ClientLauncher.ClientLaunchContext context;

    public ClientLauncherPostInitPhase(ClientLauncher.ClientLaunchContext context) {
        this.context = context;
    }
}
