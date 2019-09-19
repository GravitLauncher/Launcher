package pro.gravit.launcher.client.events;

import pro.gravit.launcher.client.ClientLauncher;
import pro.gravit.launcher.modules.events.InitPhase;

public class ClientLauncherInitPhase extends InitPhase {
    public final ClientLauncher.ClientLaunchContext context;

    public ClientLauncherInitPhase(ClientLauncher.ClientLaunchContext context) {
        this.context = context;
    }
}
