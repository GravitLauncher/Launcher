package pro.gravit.launchserver.dao.impl;

import pro.gravit.launcher.hwid.OshiHWID;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserDAO;
import pro.gravit.launchserver.dao.UserHWID;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DefaultUserDAOImpl implements UserDAO {
    public DefaultUserDAOImpl(LaunchServer srv) {
    }

    @Override
    public User findById(int id) {
        return null;
    }

    @Override
    public User findByUsername(String username) {
        return null;
    }

    @Override
    public User findByUUID(UUID uuid) {
        return null;
    }

    @Override
    public List<UserHWID> findHWID(OshiHWID hwid) {
        return new ArrayList<>();
    }

    @Override
    public void save(User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>();
    }
}
