package ru.gravit.launcher.events.request;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launcher.profiles.PlayerProfile;

public class AuthRequestEvent {
    public AuthRequestEvent() {
    }

    public String error;
    public ClientPermissions permissions;
    public PlayerProfile playerProfile;
    public String accessToken;
}
