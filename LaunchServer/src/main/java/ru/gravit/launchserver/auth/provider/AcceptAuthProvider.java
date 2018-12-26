package ru.gravit.launchserver.auth.provider;

import ru.gravit.utils.helper.SecurityHelper;

public final class AcceptAuthProvider extends AuthProvider {

    @Override
    public AuthProviderResult auth(String login, String password, String ip) {
        return new AuthProviderResult(login, SecurityHelper.randomStringToken()); // Same as login
    }

    @Override
    public void close() {
        // Do nothing
    }
}
