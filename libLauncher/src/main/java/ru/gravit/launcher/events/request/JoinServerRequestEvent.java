package ru.gravit.launcher.events.request;

import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class JoinServerRequestEvent implements EventInterface, ResultInterface {
    private static final UUID uuid = UUID.fromString("2a12e7b5-3f4a-4891-a2f9-ea141c8e1995");

    public JoinServerRequestEvent(boolean allow) {
        this.allow = allow;
    }

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
