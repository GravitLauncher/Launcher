package pro.gravit.launchserver.dao;

import java.util.function.Supplier;

public class LaunchServerDaoFactory {
    private static Supplier<UserDAO> getUserDao;

    public static void setUserDaoProvider(Supplier<UserDAO> getDao) {
        LaunchServerDaoFactory.getUserDao = getDao;
    }

    public static UserDAO createUserDao()
    {
        return getUserDao.get();
    }
}
