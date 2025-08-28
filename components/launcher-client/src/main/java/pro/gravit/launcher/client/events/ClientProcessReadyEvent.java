package pro.gravit.launcher.client.events;

import pro.gravit.launcher.client.ClientParams;
import pro.gravit.launcher.base.modules.events.PostInitPhase;

public class ClientProcessReadyEvent extends PostInitPhase {
    public final ClientParams params;

    public ClientProcessReadyEvent(ClientParams params) {
        this.params = params;
    }
}
