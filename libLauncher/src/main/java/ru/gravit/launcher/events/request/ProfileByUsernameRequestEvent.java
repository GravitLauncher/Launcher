package ru.gravit.launcher.events.request;

import ru.gravit.launcher.profiles.PlayerProfile;

public class ProfileByUsernameRequestEvent
{
    String requesttype = "profileByUsername";
    String error;
    PlayerProfile playerProfile;

    public ProfileByUsernameRequestEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }
}
