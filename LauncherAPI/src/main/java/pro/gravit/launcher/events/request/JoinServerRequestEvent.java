package pro.gravit.launcher.events.request;

import java.util.UUID;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;


public class JoinServerRequestEvent extends RequestEvent {
    @SuppressWarnings("unused")
	private static final UUID uuid = UUID.fromString("2a12e7b5-3f4a-4891-a2f9-ea141c8e1995");

    public JoinServerRequestEvent(boolean allow) {
        this.allow = allow;
    }

    @LauncherNetworkAPI
    public final boolean allow;

    @Override
    public String getType() {
        return "joinServer";
    }
}
