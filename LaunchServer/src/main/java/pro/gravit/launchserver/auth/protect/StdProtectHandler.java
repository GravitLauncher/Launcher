package pro.gravit.launchserver.auth.protect;

import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

public class StdProtectHandler extends ProtectHandler {
    public boolean checkSecure = true;
    @Override
    public String generateSecureToken(AuthResponse.AuthContext context) {
        return SecurityHelper.randomStringToken();
    }

    @Override
    public String generateClientSecureToken() {
        return SecurityHelper.randomStringToken();
    }

    @Override
    public boolean verifyClientSecureToken(String token, String secureKey) {
        return true;
    }

    @Override
    public boolean allowGetAccessToken(AuthResponse.AuthContext context) {
        return (context.authType == AuthResponse.ConnectTypes.CLIENT) && (!checkSecure || context.client.isSecure);
    }

    @Override
    public void checkLaunchServerLicense() {

    }
}
