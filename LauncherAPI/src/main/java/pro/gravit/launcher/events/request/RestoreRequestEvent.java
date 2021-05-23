package pro.gravit.launcher.events.request;

import pro.gravit.launcher.events.RequestEvent;

import java.util.List;

public class RestoreRequestEvent extends RequestEvent {
    public CurrentUserRequestEvent.UserInfo userInfo;
    public List<String> invalidTokens;

    public RestoreRequestEvent() {
    }

    public RestoreRequestEvent(CurrentUserRequestEvent.UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public RestoreRequestEvent(CurrentUserRequestEvent.UserInfo userInfo, List<String> invalidTokens) {
        this.userInfo = userInfo;
        this.invalidTokens = invalidTokens;
    }

    public RestoreRequestEvent(List<String> invalidTokens) {
        this.invalidTokens = invalidTokens;
    }

    @Override
    public String getType() {
        return "restore";
    }
}
