package pro.gravit.launcher.base.events.request;

import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launcher.base.profiles.PlayerProfile;

import java.util.Map;
import java.util.UUID;


public class CheckServerRequestEvent extends RequestEvent {
    @SuppressWarnings("unused")
    private static final UUID _uuid = UUID.fromString("8801d07c-51ba-4059-b61d-fe1f1510b28a");
    @LauncherNetworkAPI
    public UUID uuid;
    @LauncherNetworkAPI
    public PlayerProfile playerProfile;
    @LauncherNetworkAPI
    public String sessionId;
    @LauncherNetworkAPI
    public String hardwareId;
    @LauncherNetworkAPI
    public Map<String, String> sessionProperties;

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
