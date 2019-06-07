package pro.gravit.launchserver.auth.provider;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.LaunchServer;


public class AuthProviderResult {
    public final String username;
    public final String accessToken;
    public final ClientPermissions permissions;

    public AuthProviderResult(String username, String accessToken, LaunchServer server) {
        this.username = username;
        this.accessToken = accessToken;
        permissions = server.config.permissionsHandler.getPermissions(username);
    }

    public AuthProviderResult(String username, String accessToken, ClientPermissions permissions) {
        this.username = username;
        this.accessToken = accessToken;
        this.permissions = permissions;
    }
}
