package pro.gravit.launcher.client.events;

import pro.gravit.launcher.client.ClientParams;
import pro.gravit.launcher.base.modules.events.InitPhase;

public class ClientProcessInitPhase extends InitPhase {
    public final ClientParams params;

    public ClientProcessInitPhase(ClientParams params) {
        this.params = params;
    }
}
