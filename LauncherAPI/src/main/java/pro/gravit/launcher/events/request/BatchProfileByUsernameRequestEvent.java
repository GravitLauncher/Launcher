package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.utils.event.EventInterface;
import pro.gravit.launcher.events.RequestEvent;

import java.util.UUID;

public class BatchProfileByUsernameRequestEvent extends RequestEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("c1d6729e-be2c-48cc-b5ae-af8c012232c3");
    @LauncherNetworkAPI
    public String error;
    @LauncherNetworkAPI
    public PlayerProfile[] playerProfiles;

    public BatchProfileByUsernameRequestEvent(PlayerProfile[] profiles) {
        this.playerProfiles = profiles;
    }

    public BatchProfileByUsernameRequestEvent() {
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "batchProfileByUsername";
    }
}
