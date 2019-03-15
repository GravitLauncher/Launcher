package ru.gravit.launchserver.auth.protect;

import ru.gravit.launchserver.response.auth.AuthResponse;
import ru.gravit.utils.helper.SecurityHelper;

public class NoProtectHandler extends ProtectHandler {
    @Override
    public String generateSecureToken(AuthResponse.AuthContext context) {
        return SecurityHelper.randomStringToken();
    }

    @Override
    public void checkLaunchServerLicense() {
        // None
    }
}
