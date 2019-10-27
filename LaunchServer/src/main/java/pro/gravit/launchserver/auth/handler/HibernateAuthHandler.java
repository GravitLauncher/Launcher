package pro.gravit.launchserver.auth.handler;

import pro.gravit.launchserver.dao.User;

import java.util.UUID;

public class HibernateAuthHandler extends CachedAuthHandler {
    @Override
    protected Entry fetchEntry(String username) {
        User user = srv.config.dao.userService.findUserByUsername(username);
        if (user == null) return null;
        return new Entry(user.uuid, username, user.getAccessToken(), user.serverID);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) {
        User user = srv.config.dao.userService.findUserByUUID(uuid);
        if (user == null) return null;
        return new Entry(user.uuid, user.username, user.getAccessToken(), user.serverID);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) {
        User user = srv.config.dao.userService.findUserByUUID(uuid);
        user.setAccessToken(accessToken);
        srv.config.dao.userService.updateUser(user);
        return true;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) {
        User user = srv.config.dao.userService.findUserByUUID(uuid);
        user.serverID = serverID;
        srv.config.dao.userService.updateUser(user);
        return true;
    }

    @Override
    public void close() {

    }
}
