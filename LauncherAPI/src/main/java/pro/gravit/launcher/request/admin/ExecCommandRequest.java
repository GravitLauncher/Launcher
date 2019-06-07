package pro.gravit.launcher.request.admin;

import pro.gravit.launcher.LauncherAPI;
import pro.gravit.launcher.events.request.ExecCommandRequestEvent;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.websockets.RequestInterface;

public class ExecCommandRequest extends Request<ExecCommandRequestEvent> implements RequestInterface {
    @LauncherAPI
    public String cmd;

    public ExecCommandRequest(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public String getType() {
        return "cmdExec";
    }
}
