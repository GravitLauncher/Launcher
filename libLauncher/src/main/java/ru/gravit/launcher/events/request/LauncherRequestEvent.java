package ru.gravit.launcher.events.request;

import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class LauncherRequestEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("d54cc12a-4f59-4f23-9b10-f527fdd2e38f");
    public String type = "success";
    public String requesttype = "launcherUpdate";
    public String url;

    public LauncherRequestEvent(boolean needUpdate, String url) {
        this.needUpdate = needUpdate;
        this.url = url;
    }

    public boolean needUpdate;
    @Override
    public UUID getUUID() {
        return uuid;
    }
}
