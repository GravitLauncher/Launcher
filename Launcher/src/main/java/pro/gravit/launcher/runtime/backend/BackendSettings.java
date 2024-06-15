package pro.gravit.launcher.runtime.backend;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.core.backend.UserSettings;

import java.util.Map;
import java.util.UUID;

public class BackendSettings extends UserSettings {
    @LauncherNetworkAPI
    public AuthorizationData auth;
    @LauncherNetworkAPI
    public Map<UUID, ProfileSettingsImpl> settings;
    public static class AuthorizationData {
        @LauncherNetworkAPI
        public String accessToken;
        @LauncherNetworkAPI
        public String refreshToken;
        @LauncherNetworkAPI
        public long expireIn;
    }
}
