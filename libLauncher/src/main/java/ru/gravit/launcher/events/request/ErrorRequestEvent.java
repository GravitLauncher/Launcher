package ru.gravit.launcher.events.request;

import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class ErrorRequestEvent implements ResultInterface, EventInterface {
    public static UUID uuid = UUID.fromString("0af22bc7-aa01-4881-bdbb-dc62b3cdac96");
    public ErrorRequestEvent(String error) {
        this.error = error;
    }

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
