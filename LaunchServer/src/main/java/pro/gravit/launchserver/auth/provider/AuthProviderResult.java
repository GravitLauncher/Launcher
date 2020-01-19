package pro.gravit.launchserver.auth.provider;

import pro.gravit.launcher.ClientPermissions;


public class AuthProviderResult {
    public final String username;
    public final String accessToken;
    public final ClientPermissions permissions;

    public AuthProviderResult(String username, String accessToken) {
        this.username = username;
        this.accessToken = accessToken;
        permissions = ClientPermissions.DEFAULT;
    }

    public AuthProviderResult(String username, String accessToken, ClientPermissions permissions) {
        this.username = username;
        this.accessToken = accessToken;
        this.permissions = permissions;
    }
}
