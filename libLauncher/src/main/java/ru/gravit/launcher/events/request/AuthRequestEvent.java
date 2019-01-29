package ru.gravit.launcher.events.request;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class AuthRequestEvent implements EventInterface, ResultInterface {
    private static final UUID uuid = UUID.fromString("77e1bfd7-adf9-4f5d-87d6-a7dd068deb74");
    public AuthRequestEvent() {
    }

    public String error;
    public ClientPermissions permissions;
    public PlayerProfile playerProfile;
    public String accessToken;

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "auth";
    }
}
