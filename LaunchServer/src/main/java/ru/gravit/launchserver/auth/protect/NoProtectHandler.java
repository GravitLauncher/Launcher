package ru.gravit.launchserver.auth.protect;

import ru.gravit.launchserver.websocket.json.auth.AuthResponse;
import ru.gravit.utils.helper.SecurityHelper;

public class NoProtectHandler extends ProtectHandler {
    @Override
    public String generateSecureToken(AuthResponse.AuthContext context) {
        return SecurityHelper.randomStringToken();
    }

    @Override
    public String generateClientSecureToken() {
        return SecurityHelper.randomStringToken();
    }

    @Override
    public boolean verifyClientSecureToken(String token) {
        return true;
    }

    @Override
    public boolean allowGetAccessToken(AuthResponse.AuthContext context) {
        return true;
    }

    @Override
    public void checkLaunchServerLicense() {
        // None
    }
}
