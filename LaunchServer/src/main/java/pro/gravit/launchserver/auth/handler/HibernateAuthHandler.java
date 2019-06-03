package pro.gravit.launchserver.auth.handler;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.User;

import java.io.IOException;
import java.util.UUID;

public class HibernateAuthHandler extends CachedAuthHandler {
    @Override
    protected Entry fetchEntry(String username) throws IOException {
        User user = LaunchServer.server.userService.findUserByUsername(username);
        if(user  == null) return null;
        return new Entry(user.uuid, username, user.getAccessToken(), user.serverID);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        User user = LaunchServer.server.userService.findUserByUUID(uuid);
        if(user  == null) return null;
        return new Entry(user.uuid, user.username, user.getAccessToken(), user.serverID);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        User user = LaunchServer.server.userService.findUserByUUID(uuid);
        user.setAccessToken(accessToken);
        LaunchServer.server.userService.updateUser(user);
        return true;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        User user = LaunchServer.server.userService.findUserByUUID(uuid);
        user.serverID = serverID;
        LaunchServer.server.userService.updateUser(user);
        return true;
    }

    @Override
    public void close() throws IOException {

    }
}
