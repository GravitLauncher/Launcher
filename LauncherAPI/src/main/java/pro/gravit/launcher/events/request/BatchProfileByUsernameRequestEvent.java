package pro.gravit.launcher.events.request;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;

public class BatchProfileByUsernameRequestEvent extends RequestEvent {
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
    public String getType() {
        return "batchProfileByUsername";
    }
}
