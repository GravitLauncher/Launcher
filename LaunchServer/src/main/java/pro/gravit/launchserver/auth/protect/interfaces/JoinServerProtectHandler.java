package pro.gravit.launchserver.auth.protect.interfaces;

import pro.gravit.launchserver.socket.Client;

import java.util.UUID;

public interface JoinServerProtectHandler {
    default boolean onJoinServer(String serverID, String username, UUID uuid, Client client) {
        return true;
    }
}
