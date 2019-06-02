package pro.gravit.launchserver.auth.permissions;

import pro.gravit.launcher.ClientPermissions;

public class DefaultPermissionsHandler extends PermissionsHandler {
    @Override
    public void init() {

    }

    @Override
    public ClientPermissions getPermissions(String username) {
        return ClientPermissions.DEFAULT;
    }

    @Override
    public void setPermissions(String username, ClientPermissions permissions) {
        //Unsupported
    }

    @Override
    public void close() {

    }
}
