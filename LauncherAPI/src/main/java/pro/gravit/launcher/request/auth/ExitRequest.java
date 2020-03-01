package pro.gravit.launcher.request.auth;

import pro.gravit.launcher.events.request.ExitRequestEvent;
import pro.gravit.launcher.request.Request;

public class ExitRequest extends Request<ExitRequestEvent> {
    public final boolean exitAll;
    public final String username;

    public ExitRequest() {
        this.exitAll = false;
        this.username = null;
    }

    public ExitRequest(boolean exitAll) {
        this.exitAll = exitAll;
        this.username = null;
    }

    public ExitRequest(boolean exitAll, String username) {
        this.exitAll = exitAll;
        this.username = username;
    }

    @Override
    public String getType() {
        return "exit";
    }
}
