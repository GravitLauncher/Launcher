package ru.gravit.launchserver.auth.permissions;

import ru.gravit.launcher.ClientPermissions;

public class ConfigPermissionsHandler extends PermissionsHandler {
    public boolean isAdmin = false;
    public boolean isServer = false;

    @Override
    public void init() {

    }

    @Override
    public ClientPermissions getPermissions(String username) {
        ClientPermissions permissions = new ClientPermissions();
        permissions.canServer = isServer;
        permissions.canAdmin = isAdmin;
        return permissions;
    }

    @Override
    public void setPermissions(String username, ClientPermissions permissions) {
        //Unsupported
    }

    @Override
    public void close() {

    }
}
