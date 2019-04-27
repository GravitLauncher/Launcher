package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.request.ResultInterface;

public class LogEvent implements ResultInterface {
    @Override
    public String getType() {
        return "log";
    }
    @LauncherNetworkAPI
    public String string;

    public LogEvent(String string) {
        this.string = string;
    }
}
