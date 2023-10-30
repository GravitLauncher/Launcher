package pro.gravit.launcher.client.events.client;

import pro.gravit.launcher.client.ClientParams;
import pro.gravit.launcher.modules.events.InitPhase;

public class ClientProcessInitPhase extends InitPhase {
    public final ClientParams params;

    public ClientProcessInitPhase(ClientParams params) {
        this.params = params;
    }
}
