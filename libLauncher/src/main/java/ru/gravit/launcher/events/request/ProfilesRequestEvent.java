package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.event.EventInterface;

import java.util.List;
import java.util.UUID;

public class ProfilesRequestEvent implements EventInterface, ResultInterface
{
    private static final UUID uuid = UUID.fromString("2f26fbdf-598a-46dd-92fc-1699c0e173b1");
    @LauncherNetworkAPI
    public List<ClientProfile> profiles;

    public ProfilesRequestEvent(List<ClientProfile> profiles) {
        this.profiles = profiles;
    }

    public ProfilesRequestEvent() {
    }

    String error;
    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "profiles";
    }
}
