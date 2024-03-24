package pro.gravit.launchserver.auth.core;

import pro.gravit.launcher.base.ClientPermissions;

import java.util.UUID;

public interface User {
    String getUsername();

    UUID getUUID();

    ClientPermissions getPermissions();

    default boolean isBanned() {
        return false;
    }
}
