package ru.gravit.launchserver.auth.permissions;

import ru.gravit.launchserver.auth.ClientPermissions;
import ru.gravit.launchserver.socket.Client;

public class ConfigPermissionsHandler extends PermissionsHandler {
    public boolean isAdmin = false;
    public boolean isServer = false;
    @Override
    public ClientPermissions getPermissions(String username) {
        ClientPermissions permissions = new ClientPermissions();
        permissions.canServer = isServer;
        permissions.canAdmin = isAdmin;
        return permissions;
    }
}
