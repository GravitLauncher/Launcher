package ru.gravit.launchserver.auth.permissions;

import ru.gravit.launcher.ClientPermissions;

public class DefaultPermissionsHandler extends PermissionsHandler {
    @Override
    public ClientPermissions getPermissions(String username) {
        return ClientPermissions.DEFAULT;
    }

    @Override
    public void close() throws Exception {

    }
}
