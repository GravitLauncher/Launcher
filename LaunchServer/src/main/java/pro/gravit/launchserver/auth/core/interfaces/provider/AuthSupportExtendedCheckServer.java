package pro.gravit.launchserver.auth.core.interfaces.provider;

import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.socket.Client;

import java.io.IOException;

public interface AuthSupportExtendedCheckServer {
    UserSession extendedCheckServer(Client client, String username, String serverID) throws IOException;
}
