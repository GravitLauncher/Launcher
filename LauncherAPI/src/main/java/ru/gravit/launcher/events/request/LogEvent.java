package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class LogEvent extends RequestEvent implements EventInterface {
    @Override
    public String getType() {
        return "log";
    }

    @LauncherNetworkAPI
    public String string;

    public LogEvent(String string) {
        this.string = string;
    }

    @Override
    public UUID getUUID() {
        return null;
    }
}
