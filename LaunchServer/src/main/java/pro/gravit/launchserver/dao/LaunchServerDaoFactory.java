package pro.gravit.launchserver.dao;

import pro.gravit.launchserver.dao.impl.DefaultUserDAOImpl;

import java.util.function.Supplier;

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
