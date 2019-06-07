package pro.gravit.launchserver.dao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.impl.DefaultUserDAOImpl;

public class LaunchServerDaoFactory {
    private static final Function<LaunchServer, UserDAO> defDao = DefaultUserDAOImpl::new;
    private static final Map<LaunchServer, Function<LaunchServer, UserDAO>> daos = new ConcurrentHashMap<>();

    public static void setUserDaoProvider(LaunchServer srv, Function<LaunchServer, UserDAO> getDao) {
        daos.put(srv, getDao);
    }

    public static UserDAO createUserDao(LaunchServer srv)
    {
        return daos.getOrDefault(srv, defDao).apply(srv);
    }
}
