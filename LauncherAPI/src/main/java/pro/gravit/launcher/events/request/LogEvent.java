package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.request.WebSocketEvent;

public class LogEvent implements WebSocketEvent {
    @LauncherNetworkAPI
    public final String string;

    public LogEvent(String string) {
        this.string = string;
    }

    @Override
    public String getType() {
        return "log";
    }
}
