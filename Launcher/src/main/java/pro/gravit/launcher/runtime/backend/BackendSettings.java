package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.backend.UserSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackendSettings extends UserSettings {
    @LauncherNetworkAPI
    public AuthorizationData auth;
    @LauncherNetworkAPI
    public Map<UUID, ProfileSettingsImpl> settings = new HashMap<>();
    public static class AuthorizationData {
        @LauncherNetworkAPI
        public String accessToken;
        @LauncherNetworkAPI
        public String refreshToken;
        @LauncherNetworkAPI
        public long expireIn;

        public AuthFeatureAPI.AuthToken toToken() {
            return new AuthFeatureAPI.AuthToken() {
                @Override
                public String getAccessToken() {
                    return accessToken;
                }

                @Override
                public String getRefreshToken() {
                    return refreshToken;
                }

                @Override
                public long getExpire() {
                    return expireIn;
                }
            };
        }
    }
}
