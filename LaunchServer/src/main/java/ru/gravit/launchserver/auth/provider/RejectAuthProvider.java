package ru.gravit.launchserver.auth.provider;

import ru.gravit.launchserver.auth.AuthException;
public final class RejectAuthProvider extends AuthProvider {
    public RejectAuthProvider() {
    }

    public RejectAuthProvider(String message) {
        this.message = message;
    }

    private String message;

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws AuthException {
        return authError(message);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
