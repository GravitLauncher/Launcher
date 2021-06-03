package pro.gravit.launchserver.auth.core;

import pro.gravit.launcher.ClientPermissions;

import java.util.UUID;

public interface User {
    String getUsername();

    UUID getUUID();

    String getServerId();

    String getAccessToken();

    ClientPermissions getPermissions();

    default boolean isBanned() {
        return false;
    }
}
