package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;

import java.util.UUID;


public class JoinServerRequestEvent extends RequestEvent {
    @SuppressWarnings("unused")
    private static final UUID uuid = UUID.fromString("2a12e7b5-3f4a-4891-a2f9-ea141c8e1995");
    @LauncherNetworkAPI
    public final boolean allow;

    public JoinServerRequestEvent(boolean allow) {
        this.allow = allow;
    }

    @Override
    public String getType() {
        return "joinServer";
    }
}
