package pro.gravit.launchserver.auth.protect;

import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

public class NoProtectHandler extends ProtectHandler {

    @Override
    public boolean allowGetAccessToken(AuthResponse.AuthContext context) {
        return true;
    }

    @Override
    public boolean allowJoinServer(Client client) {
        return true;
    }
}
