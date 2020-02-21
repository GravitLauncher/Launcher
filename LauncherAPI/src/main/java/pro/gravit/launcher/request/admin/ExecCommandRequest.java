package pro.gravit.launcher.request.admin;

import pro.gravit.launcher.events.request.ExecCommandRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.WebSocketRequest;

public class ExecCommandRequest extends Request<ExecCommandRequestEvent> implements WebSocketRequest {

    public final String cmd;

    public ExecCommandRequest(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public String getType() {
        return "cmdExec";
    }
}
