package ru.gravit.launcher.events.request;

import ru.gravit.launcher.LauncherNetworkAPI;
import ru.gravit.launcher.profiles.PlayerProfile;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.event.EventInterface;

import java.util.UUID;

public class CheckServerRequestEvent implements EventInterface, ResultInterface {
    private static final UUID _uuid = UUID.fromString("8801d07c-51ba-4059-b61d-fe1f1510b28a");
    @LauncherNetworkAPI
    public UUID uuid;
    @LauncherNetworkAPI
    public PlayerProfile playerProfile;

    public CheckServerRequestEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    public CheckServerRequestEvent() {
    }

    @Override
    public UUID getUUID() {
        return _uuid;
    }

    @Override
    public String getType() {
        return "checkServe";
    }
}
