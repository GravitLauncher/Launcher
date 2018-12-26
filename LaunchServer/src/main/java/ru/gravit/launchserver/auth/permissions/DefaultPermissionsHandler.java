package ru.gravit.launchserver.auth.permissions;

import ru.gravit.launchserver.auth.ClientPermissions;

public class DefaultPermissionsHandler extends PermissionsHandler {
    @Override
    public ClientPermissions getPermissions(String username) {
        return ClientPermissions.DEFAULT;
    }
}
