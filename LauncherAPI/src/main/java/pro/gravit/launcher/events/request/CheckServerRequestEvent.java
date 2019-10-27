package pro.gravit.launcher.events.request;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;

import java.util.UUID;


public class CheckServerRequestEvent extends RequestEvent {
    @SuppressWarnings("unused")
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
    public String getType() {
        return "checkServer";
    }
}
