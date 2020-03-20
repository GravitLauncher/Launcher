package pro.gravit.launchserver.auth.protect;

import pro.gravit.launcher.events.request.GetSecureLevelInfoRequestEvent;
import pro.gravit.launchserver.auth.protect.interfaces.SecureProtectHandler;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

public class AdvancedProtectHandler extends ProtectHandler implements SecureProtectHandler {
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
        return (context.authType == AuthResponse.ConnectTypes.CLIENT) && context.client.isSecure;
    }

    @Override
    public void checkLaunchServerLicense() {

    }

    @Override
    public GetSecureLevelInfoRequestEvent onGetSecureLevelInfo(GetSecureLevelInfoRequestEvent event) {
        return event;
    }

    @Override
    public boolean allowGetSecureLevelInfo(Client client) {
        return client.isSecure;
    }
}
