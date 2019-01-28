package ru.gravit.launcher.events.request;

import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class CheckServerEvent implements EventInterface {
    private static final UUID _uuid = UUID.fromString("8801d07c-51ba-4059-b61d-fe1f1510b28a");
    public String type = "success";
    public String requesttype = "checkServer";
    public UUID uuid;
    public PlayerProfile playerProfile;
    @Override
    public UUID getUUID() {
        return _uuid;
    }
}
