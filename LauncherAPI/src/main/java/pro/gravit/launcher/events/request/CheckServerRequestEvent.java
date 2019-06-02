package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.utils.event.EventInterface;
import pro.gravit.launcher.events.RequestEvent;

import java.util.UUID;

public class CheckServerRequestEvent extends RequestEvent implements EventInterface {
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
        return "checkServer";
    }
}
