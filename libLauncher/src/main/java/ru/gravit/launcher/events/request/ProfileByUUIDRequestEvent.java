package ru.gravit.launcher.events.request;

import ru.gravit.launcher.profiles.PlayerProfile;

public class ProfileByUUIDRequestEvent
{
    String requesttype = "profileByUUID";
    String error;
    PlayerProfile playerProfile;

    public ProfileByUUIDRequestEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }
}
