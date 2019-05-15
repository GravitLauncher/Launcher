package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class ErrorRequestEvent extends RequestEvent implements EventInterface {
    public static UUID uuid = UUID.fromString("0af22bc7-aa01-4881-bdbb-dc62b3cdac96");

    public ErrorRequestEvent(String error) {
        this.error = error;
    }

    @LauncherNetworkAPI
    public final String error;

    @Override
    public String getType() {
        return "error";
    }

    @Override
    public UUID getUUID() {
        return null;
    }
}
