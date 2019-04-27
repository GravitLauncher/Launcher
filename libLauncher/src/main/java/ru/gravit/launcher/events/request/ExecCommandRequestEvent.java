package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.events.RequestEvent;

public class ExecCommandRequestEvent extends RequestEvent {
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
