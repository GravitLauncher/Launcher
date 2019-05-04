package ru.gravit.launcher.request.admin;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.events.request.ExecCommandRequestEvent;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.websockets.RequestInterface;

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
