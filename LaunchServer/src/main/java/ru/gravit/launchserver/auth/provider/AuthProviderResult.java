package ru.gravit.launchserver.auth.provider;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launchserver.LaunchServer;


public class AuthProviderResult {
    public final String username;
    public final String accessToken;
    public final ClientPermissions permissions;

    public AuthProviderResult(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;
        permissions = LaunchServer.server.config.permissionsHandler.getPermissions(username);
    }

    public AuthProviderResult(String username, String accessToken, ClientPermissions permissions) {
        this.username = username;
        this.accessToken = accessToken;
        this.permissions = permissions;
    }
}
