package pro.gravit.launchserver.dao.provider;

import java.nio.file.Paths;

import org.hibernate.cfg.Configuration;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserHWID;
import pro.gravit.launchserver.dao.UserService;
import pro.gravit.launchserver.dao.impl.HibernateUserDAOImpl;
import pro.gravit.utils.helper.CommonHelper;

public class HibernateDaoProvider extends DaoProvider {
    public String driver;
    public String url;
    public String username;
    public String password;
    public String pool_size;
    public String hibernateConfig;
    public boolean parallelHibernateInit;

    @Override
    public void init(LaunchServer server) {
        Runnable init = () -> {
            Configuration cfg = new Configuration()
                    .addAnnotatedClass(User.class)
                    .addAnnotatedClass(UserHWID.class)
                    .setProperty("hibernate.connection.driver_class", driver)
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.connection.username", username)
                    .setProperty("hibernate.connection.password", password)
                    .setProperty("hibernate.connection.pool_size", pool_size);
            if(hibernateConfig != null)
                cfg.configure(Paths.get(hibernateConfig).toFile());
            userDAO = new HibernateUserDAOImpl(cfg.buildSessionFactory());
            userService = new UserService(userDAO);
        };
        if(parallelHibernateInit)
            CommonHelper.newThread("Hibernate Thread", true, init);
        else
            init.run();
    }
}
