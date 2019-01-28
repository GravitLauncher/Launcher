package ru.gravit.launcher.events.request;

import ru.gravit.launcher.hasher.HashedDir;

public class UpdateListRequestEvent {
    public final String type;
    public final String requesttype;
    public final HashedDir dir;

    public UpdateListRequestEvent(HashedDir dir) {
        this.dir = dir;
        type = "success";
        requesttype = "updateList";
    }
}
