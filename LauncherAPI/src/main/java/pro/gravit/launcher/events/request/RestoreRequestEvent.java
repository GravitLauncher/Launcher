package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class RestoreRequestEvent extends RequestEvent {
    public CurrentUserRequestEvent.UserInfo userInfo;

    public RestoreRequestEvent() {
    }

    public RestoreRequestEvent(CurrentUserRequestEvent.UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String getType() {
        return "restore";
    }
}
