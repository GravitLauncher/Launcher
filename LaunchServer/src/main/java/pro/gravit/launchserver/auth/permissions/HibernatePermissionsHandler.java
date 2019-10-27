package pro.gravit.launchserver.auth.permissions;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.dao.User;

public class HibernatePermissionsHandler extends PermissionsHandler {

    @Override
    public ClientPermissions getPermissions(String username) {
        User user = srv.config.dao.userService.findUserByUsername(username);
        if (user == null) return ClientPermissions.DEFAULT;
        return user.getPermissions();
    }

    @Override
    public void setPermissions(String username, ClientPermissions permissions) {
        User user = srv.config.dao.userService.findUserByUsername(username);
        if (user == null) return;
        user.setPermissions(permissions);
        srv.config.dao.userService.updateUser(user);
    }

    @Override
    public void close() {

    }
}
