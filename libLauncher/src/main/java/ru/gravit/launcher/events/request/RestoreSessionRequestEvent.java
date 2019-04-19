package ru.gravit.launcher.events.request;

import ru.gravit.launcher.events.RequestEvent;

public class RestoreSessionRequestEvent extends RequestEvent {
    @Override
    public String getType() {
        return "restoreSession";
    }
}
