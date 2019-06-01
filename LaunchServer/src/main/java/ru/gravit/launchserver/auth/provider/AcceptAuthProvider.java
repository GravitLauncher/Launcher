package ru.gravit.launchserver.auth.provider;

import ru.gravit.utils.helper.SecurityHelper;

public final class AcceptAuthProvider extends AuthProvider {

    @Override
    public AuthProviderResult auth(String login, String password, String ip) {
        return new AuthProviderResult(login, SecurityHelper.randomStringToken()); // Same as login
    }

    @Override
    public AuthProviderResult oauth(int i) throws Exception {
        return new AuthProviderResult(String.valueOf(i), SecurityHelper.randomStringToken()); // Same as login
    }

    @Override
    public void close() {
        // Do nothing
    }
}
