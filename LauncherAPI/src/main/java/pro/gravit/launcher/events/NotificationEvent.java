package pro.gravit.launcher.events;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.request.WebSocketEvent;

public class NotificationEvent implements WebSocketEvent {
    @LauncherNetworkAPI
    public final String head;
    @LauncherNetworkAPI
    public final String message;

    public NotificationEvent(String head, String message) {
        this.head = head;
        this.message = message;
    }

    @Override
    public String getType() {
        return "notification";
    }
}
