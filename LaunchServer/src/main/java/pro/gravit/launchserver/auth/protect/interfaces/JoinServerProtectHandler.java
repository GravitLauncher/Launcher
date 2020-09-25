package pro.gravit.launchserver.auth.protect.interfaces;

import pro.gravit.launchserver.socket.Client;

public interface JoinServerProtectHandler {
    default boolean onJoinServer(String serverID, String username, Client client) {
        return true;
    }
}
