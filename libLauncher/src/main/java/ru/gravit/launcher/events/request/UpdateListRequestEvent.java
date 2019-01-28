package ru.gravit.launcher.events.request;

import ru.gravit.launcher.hasher.HashedDir;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class UpdateListRequestEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("5fa836ae-6b61-401c-96ac-d8396f07ec6b");
    public final String type;
    public final String requesttype;
    public final HashedDir dir;

    public UpdateListRequestEvent(HashedDir dir) {
        this.dir = dir;
        type = "success";
        requesttype = "updateList";
    }
    @Override
    public UUID getUUID() {
        return uuid;
    }
}
