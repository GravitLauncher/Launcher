package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;

import java.util.Map;
import java.util.UUID;

public class CurrentUserRequestEvent extends RequestEvent {
    public final UserInfo userInfo;

    public CurrentUserRequestEvent(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String getType() {
        return "currentUser";
    }

    public static class UserInfo implements SelfUser {
        public ClientPermissions permissions;
        public String accessToken;
        public PlayerProfile playerProfile;

        public UserInfo() {
        }

        public UserInfo(ClientPermissions permissions, String accessToken, PlayerProfile playerProfile) {
            this.permissions = permissions;
            this.accessToken = accessToken;
            this.playerProfile = playerProfile;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public ClientPermissions getPermissions() {
            return permissions;
        }

        @Override
        public String getUsername() {
            return playerProfile.getUsername();
        }

        @Override
        public UUID getUUID() {
            return playerProfile.getUUID();
        }

        @Override
        public Map<String, Texture> getAssets() {
            return playerProfile.getAssets();
        }

        @Override
        public Map<String, String> getProperties() {
            return playerProfile.getProperties();
        }
    }
}
