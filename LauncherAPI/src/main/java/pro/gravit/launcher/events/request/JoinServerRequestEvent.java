package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.utils.event.EventInterface;
import pro.gravit.launcher.events.RequestEvent;

import java.util.UUID;

public class JoinServerRequestEvent extends RequestEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("2a12e7b5-3f4a-4891-a2f9-ea141c8e1995");

    public JoinServerRequestEvent(boolean allow) {
        this.allow = allow;
    }

    @LauncherNetworkAPI
    public boolean allow;

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "joinServer";
    }
}
