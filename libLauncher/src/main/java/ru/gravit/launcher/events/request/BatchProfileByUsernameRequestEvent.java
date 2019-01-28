package ru.gravit.launcher.events.request;

import ru.gravit.launcher.profiles.PlayerProfile;

public class BatchProfileByUsernameRequestEvent
{
    public String requesttype = "batchProfileByUsername";
    public String error;
    public PlayerProfile[] playerProfiles;
}
