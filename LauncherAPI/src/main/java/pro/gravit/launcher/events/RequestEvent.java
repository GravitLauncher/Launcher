package pro.gravit.launcher.events;

import java.util.UUID;

import pro.gravit.launcher.request.WebSocketEvent;

public abstract class RequestEvent implements WebSocketEvent {
    public UUID requestUUID;
}
