package pro.gravit.launcher.client.events;

import pro.gravit.launcher.base.modules.events.ClosePhase;

public class ClientExitPhase extends ClosePhase {
    public final int code;

    public ClientExitPhase(int code) {
        this.code = code;
    }
}
