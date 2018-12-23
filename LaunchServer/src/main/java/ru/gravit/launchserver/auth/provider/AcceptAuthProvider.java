package ru.gravit.launchserver.auth.provider;

import ru.gravit.launchserver.auth.ClientPermissions;
import ru.gravit.utils.helper.SecurityHelper;

public final class AcceptAuthProvider extends AuthProvider {
    private boolean isAdminAccess;

    @Override
    public AuthProviderResult auth(String login, String password, String ip) {
        return new AuthProviderResult(login, SecurityHelper.randomStringToken(), isAdminAccess ? ClientPermissions.getSuperuserAccount() : ClientPermissions.DEFAULT); // Same as login
    }

    @Override
    public void close() {
        // Do nothing
    }
}
