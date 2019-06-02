package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.request.ResultInterface;

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
