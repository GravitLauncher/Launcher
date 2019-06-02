package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.utils.event.EventInterface;
import pro.gravit.launcher.events.RequestEvent;

import java.util.UUID;

public class ProfileByUsernameRequestEvent extends RequestEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("06204302-ff6b-4779-b97d-541e3bc39aa1");
    @LauncherNetworkAPI
    public String error;
    @LauncherNetworkAPI
    public PlayerProfile playerProfile;

    public ProfileByUsernameRequestEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "profileByUsername";
    }
}
