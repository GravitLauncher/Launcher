package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.base.profiles.PlayerProfile;

import java.util.UUID;


public class ProfileByUsernameRequestEvent extends RequestEvent {
    @SuppressWarnings("unused")
    private static final UUID uuid = UUID.fromString("06204302-ff6b-4779-b97d-541e3bc39aa1");
    @LauncherNetworkAPI
    public final PlayerProfile playerProfile;
    @LauncherNetworkAPI
    public String error;

    public ProfileByUsernameRequestEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    @Override
    public String getType() {
        return "profileByUsername";
    }
}
