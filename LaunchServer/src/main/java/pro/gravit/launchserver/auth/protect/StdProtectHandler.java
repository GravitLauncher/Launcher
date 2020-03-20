package pro.gravit.launchserver.auth.protect;

import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

public class StdProtectHandler extends ProtectHandler {
    public final boolean checkSecure = true;

    @Override
    public boolean allowGetAccessToken(AuthResponse.AuthContext context) {
        return (context.authType == AuthResponse.ConnectTypes.CLIENT) && (!checkSecure || context.client.isSecure);
    }

    @Override
    public void checkLaunchServerLicense() {

    }
}
