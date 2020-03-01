package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class ExitRequestEvent extends RequestEvent {
    public enum ExitReason
    {
        SERVER, CLIENT, TIMEOUT, NO_EXIT
    }
    public final ExitReason reason;

    public ExitRequestEvent(ExitReason reason) {
        this.reason = reason;
    }

    @Override
    public String getType() {
        return "exit";
    }
}
