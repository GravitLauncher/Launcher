package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.base.profiles.PlayerProfile;

public class CurrentUserRequestEvent extends RequestEvent {
    public final UserInfo userInfo;

    public CurrentUserRequestEvent(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String getType() {
        return "currentUser";
    }

    public static class UserInfo {
        public ClientPermissions permissions;
        public String accessToken;
        public PlayerProfile playerProfile;
    }
}
