package ru.gravit.launchserver.auth.provider;

import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.utils.helper.SecurityHelper;

public final class RejectAuthProvider extends AuthProvider {
    public RejectAuthProvider() {
    }

    public RejectAuthProvider(String message) {
        this.message = message;
    }

    private String message;
    private String[] whitelist;

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws AuthException {
        if(whitelist != null)
        {
            for(String username : whitelist)
            {
                if(login.equals(username))
                {
                    return new AuthProviderResult(login, SecurityHelper.randomStringToken());
                }
            }
        }
        return authError(message);
    }

    @Override
    public void close() {
        // Do nothing
    }
}
