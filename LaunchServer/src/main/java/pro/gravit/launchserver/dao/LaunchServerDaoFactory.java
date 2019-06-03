package pro.gravit.launchserver.dao;

import java.util.function.Supplier;

import pro.gravit.launchserver.dao.impl.DefaultUserDAOImpl;

public class LaunchServerDaoFactory {
    private static Supplier<UserDAO> getUserDao = DefaultUserDAOImpl::new;

    public static void setUserDaoProvider(Supplier<UserDAO> getDao) {
        LaunchServerDaoFactory.getUserDao = getDao;
    }

    public static UserDAO createUserDao()
    {
        return getUserDao.get();
    }
}
