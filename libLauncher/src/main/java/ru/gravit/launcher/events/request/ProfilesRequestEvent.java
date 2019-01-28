package ru.gravit.launcher.events.request;

import ru.gravit.launcher.profiles.ClientProfile;

import java.util.List;

public class ProfilesRequestEvent
{
    List<ClientProfile> profiles;

    public ProfilesRequestEvent(List<ClientProfile> profiles) {
        this.profiles = profiles;
    }

    String requesttype = "profilesList";
    String error;
}
