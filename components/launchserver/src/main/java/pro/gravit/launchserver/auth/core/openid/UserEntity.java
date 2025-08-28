package pro.gravit.launchserver.auth.core.openid;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launchserver.auth.core.User;

import java.util.UUID;

record UserEntity(String username, UUID uuid, ClientPermissions permissions) implements User {
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public ClientPermissions getPermissions() {
        return permissions;
    }
}
