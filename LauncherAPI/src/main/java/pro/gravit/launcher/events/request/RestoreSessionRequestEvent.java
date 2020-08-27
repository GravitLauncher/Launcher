package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

public class RestoreSessionRequestEvent extends RequestEvent {
    public CurrentUserRequestEvent.UserInfo userInfo;

    public RestoreSessionRequestEvent() {
    }

    public RestoreSessionRequestEvent(CurrentUserRequestEvent.UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String getType() {
        return "restoreSession";
    }
}
