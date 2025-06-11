package pro.gravit.launchserver.auth.protect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

public class StdProtectHandler extends ProtectHandler {
    private transient final Logger logger = LogManager.getLogger();

    @Override
    public boolean allowGetAccessToken(AuthResponse.AuthContext context) {
        return (context.authType == AuthResponse.ConnectTypes.CLIENT) && context.client.checkSign;
    }

    @Override
    public void init(LaunchServer server) {

    }
}
