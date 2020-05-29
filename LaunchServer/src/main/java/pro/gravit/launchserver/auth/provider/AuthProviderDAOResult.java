package pro.gravit.launchserver.auth.provider;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.dao.User;

public class AuthProviderDAOResult extends AuthProviderResult {
    public User daoObject;

    public AuthProviderDAOResult(String username, String accessToken) {
        super(username, accessToken);
    }

    public AuthProviderDAOResult(String username, String accessToken, ClientPermissions permissions) {
        super(username, accessToken, permissions);
    }

    public AuthProviderDAOResult(String username, String accessToken, ClientPermissions permissions, User daoObject) {
        super(username, accessToken, permissions);
        this.daoObject = daoObject;
    }
}
