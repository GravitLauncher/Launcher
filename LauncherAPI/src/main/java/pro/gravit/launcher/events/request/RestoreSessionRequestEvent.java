package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class RestoreSessionRequestEvent extends RequestEvent {
    @Override
    public String getType() {
        return "restoreSession";
    }
}
