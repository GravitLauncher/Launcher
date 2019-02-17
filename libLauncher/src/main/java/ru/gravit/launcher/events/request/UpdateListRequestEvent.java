package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.event.EventInterface;

import java.util.HashSet;
import java.util.UUID;

public class UpdateListRequestEvent implements EventInterface, ResultInterface {
    private static final UUID uuid = UUID.fromString("5fa836ae-6b61-401c-96ac-d8396f07ec6b");
    @LauncherNetworkAPI
    public final HashSet<String> dirs;

    public UpdateListRequestEvent(HashSet<String> dirs) {
        this.dirs = dirs;
    }
    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "updateList";
    }
}
