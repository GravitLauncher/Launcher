package ru.gravit.launcher.events.request;

import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class JoinServerRequestEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("2a12e7b5-3f4a-4891-a2f9-ea141c8e1995");
    public String type = "success";
    public String requesttype = "checkServer";

    public JoinServerRequestEvent(boolean allow) {
        this.allow = allow;
    }

    public boolean allow;
    @Override
    public UUID getUUID() {
        return uuid;
    }
}
