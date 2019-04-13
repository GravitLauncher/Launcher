package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.request.ResultInterface;

public class ExecCommandRequestEvent implements ResultInterface {
    @Override
    public String getType() {
        return "cmdExec";
    }
    @LauncherNetworkAPI
    public boolean success;

    public ExecCommandRequestEvent(boolean success) {
        this.success = success;
    }
}
