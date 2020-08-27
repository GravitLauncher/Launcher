package pro.gravit.launcher.events.request;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;

public class CurrentUserRequestEvent extends RequestEvent {
    public static class UserInfo {
        public ClientPermissions permissions;
        public String accessToken;
        public PlayerProfile playerProfile;
    }
    public UserInfo userInfo;

    public CurrentUserRequestEvent(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String getType() {
        return "currentUser";
    }
}
