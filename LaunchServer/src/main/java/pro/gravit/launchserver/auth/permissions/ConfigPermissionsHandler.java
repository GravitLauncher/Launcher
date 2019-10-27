package pro.gravit.launchserver.auth.permissions;

import pro.gravit.launcher.ClientPermissions;

public class ConfigPermissionsHandler extends PermissionsHandler {
    public final boolean isAdmin = false;
    public final boolean isServer = false;

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
