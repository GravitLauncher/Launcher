package ru.gravit.launcher.events.request;

import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class BatchProfileByUsernameRequestEvent implements EventInterface
{
    private static final UUID uuid = UUID.fromString("c1d6729e-be2c-48cc-b5ae-af8c012232c3");
    public String requesttype = "batchProfileByUsername";
    public String error;
    public PlayerProfile[] playerProfiles;
    @Override
    public UUID getUUID() {
        return uuid;
    }
}
