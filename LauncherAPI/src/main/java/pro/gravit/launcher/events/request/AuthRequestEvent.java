package pro.gravit.launcher.events.request;

import java.util.UUID;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.utils.event.EventInterface;

public class AuthRequestEvent extends RequestEvent implements EventInterface {
    private static final UUID uuid = UUID.fromString("77e1bfd7-adf9-4f5d-87d6-a7dd068deb74");

    public AuthRequestEvent() {
    }

    @LauncherNetworkAPI
    public String error;
    @LauncherNetworkAPI
    public ClientPermissions permissions;
    @LauncherNetworkAPI
    public PlayerProfile playerProfile;
    @LauncherNetworkAPI
    public String accessToken;
    @LauncherNetworkAPI
    public String protectToken;
    @LauncherNetworkAPI
    public long session;

    public AuthRequestEvent(PlayerProfile pp, String accessToken, ClientPermissions permissions) {
        this.playerProfile = pp;
        this.accessToken = accessToken;
        this.permissions = permissions;
    }

    public AuthRequestEvent(ClientPermissions permissions, PlayerProfile playerProfile, String accessToken, String protectToken) {
        this.permissions = permissions;
        this.playerProfile = playerProfile;
        this.accessToken = accessToken;
        this.protectToken = protectToken;
    }

    public AuthRequestEvent(String error, ClientPermissions permissions, PlayerProfile playerProfile, String accessToken, String protectToken, long session) {
        this.error = error;
        this.permissions = permissions;
        this.playerProfile = playerProfile;
        this.accessToken = accessToken;
        this.protectToken = protectToken;
        this.session = session;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getType() {
        return "auth";
    }
}
