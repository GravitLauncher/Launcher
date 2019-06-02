package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.utils.event.EventInterface;
import pro.gravit.launcher.events.RequestEvent;

import java.util.UUID;

public class ProfileByUUIDRequestEvent extends RequestEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("b9014cf3-4b95-4d38-8c5f-867f190a18a0");
    @LauncherNetworkAPI
    public String error;
    @LauncherNetworkAPI
    public PlayerProfile playerProfile;

    public ProfileByUUIDRequestEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    public ProfileByUUIDRequestEvent() {
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "profileByUUID";
    }
}
