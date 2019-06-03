package pro.gravit.launchserver.auth.handler;

import pro.gravit.launchserver.hibernate.User;
import pro.gravit.launchserver.hibernate.UserService;

import java.io.IOException;
import java.util.UUID;

public class HibernateAuthHandler extends CachedAuthHandler {
    @Override
    protected Entry fetchEntry(String username) throws IOException {
        UserService service = new UserService();
        User user = service.findUserByUsername(username);
        if(user  == null) return null;
        return new Entry(user.uuid, username, user.getAccessToken(), user.serverID);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        UserService service = new UserService();
        User user = service.findUserByUUID(uuid);
        if(user  == null) return null;
        return new Entry(user.uuid, user.username, user.getAccessToken(), user.serverID);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        UserService service = new UserService();
        User user = service.findUserByUUID(uuid);
        user.setAccessToken(accessToken);
        service.updateUser(user);
        return true;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        UserService service = new UserService();
        User user = service.findUserByUUID(uuid);
        user.serverID = serverID;
        service.updateUser(user);
        return true;
    }

    @Override
    public void close() throws IOException {

    }
}
