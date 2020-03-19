package pro.gravit.launchserver.dao.provider;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.impl.UserHibernateImpl;
import pro.gravit.launchserver.dao.impl.HibernateUserDAOImpl;
import pro.gravit.utils.helper.CommonHelper;

import java.nio.file.Paths;

public class HibernateDaoProvider extends DaoProvider {
    public String driver;
    public String url;
    public String username;
    public String password;
    public String dialect;
    public String pool_size;
    public String hibernateConfig;
    public boolean parallelHibernateInit;
    private transient SessionFactory sessionFactory;

    @Override
    public void init(LaunchServer server) {
        Runnable init = () -> {
            Configuration cfg = new Configuration()
                    .addAnnotatedClass(UserHibernateImpl.class)
                    .setProperty("hibernate.connection.driver_class", driver)
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.connection.username", username)
                    .setProperty("hibernate.connection.password", password)
                    .setProperty("hibernate.connection.pool_size", pool_size);
            if (dialect != null)
                cfg.setProperty("hibernate.dialect", dialect);
            if (hibernateConfig != null)
                cfg.configure(Paths.get(hibernateConfig).toFile());
            sessionFactory = cfg.buildSessionFactory();
            userDAO = new HibernateUserDAOImpl(sessionFactory);
        };
        if (parallelHibernateInit)
            CommonHelper.newThread("Hibernate Thread", true, init);
        else
            init.run();
    }
}
