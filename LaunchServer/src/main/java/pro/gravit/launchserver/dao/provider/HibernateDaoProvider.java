package pro.gravit.launchserver.dao.provider;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserDAO;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Deprecated from 5.2.0
 */
@Deprecated
public abstract class HibernateDaoProvider extends DaoProvider implements Reconfigurable, AutoCloseable {
    public String driver;
    public String url;
    public String username;
    public String password;
    public String dialect;
    public String pool_size;
    public String hibernateConfig;
    public boolean parallelHibernateInit;
    protected transient SessionFactory sessionFactory;

    @Override
    public void init(LaunchServer server) {
        Runnable init = () -> {
            Configuration cfg = new Configuration()
                    //.addAnnotatedClass(UserHibernateImpl.class)
                    .setProperty("hibernate.connection.driver_class", driver)
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.connection.username", username)
                    .setProperty("hibernate.connection.password", password)
                    .setProperty("hibernate.connection.pool_size", pool_size);
            if (dialect != null)
                cfg.setProperty("hibernate.dialect", dialect);
            if (hibernateConfig != null)
                cfg.configure(Paths.get(hibernateConfig).toFile());
            onConfigure(cfg);
            sessionFactory = cfg.buildSessionFactory();
            userDAO = newUserDAO();
        };
        if (parallelHibernateInit)
            CommonHelper.newThread("Hibernate Thread", true, init);
        else
            init.run();
    }

    protected abstract void onConfigure(Configuration configuration);

    protected abstract UserDAO newUserDAO();

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("getallusers", new SubCommand() {
            @Override
            public void invoke(String... args) {
                int count = 0;
                for (User user : userDAO.findAll()) {
                    LogHelper.subInfo("[%s] UUID: %s", user.getUsername(), user.getUuid().toString());
                    count++;
                }
                LogHelper.info("Print %d users", count);
            }
        });
        commands.put("getuser", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                User user = userDAO.findByUsername(args[0]);
                if (user == null) {
                    LogHelper.error("User %s not found", args[0]);
                    return;
                }
                LogHelper.info("[%s] UUID: %s | permissions %s", user.getUsername(), user.getUuid().toString(), user.getPermissions() == null ? "null" : user.getPermissions().toString());
            }
        });
        commands.put("givepermission", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 3);
                User user = userDAO.findByUsername(args[0]);
                if (user == null) {
                    LogHelper.error("User %s not found", args[0]);
                    return;
                }
                ClientPermissions permissions = user.getPermissions();
                long perm = Long.parseLong(args[1]);
                boolean value = Boolean.parseBoolean(args[2]);
                permissions.setPermission(perm, value);
                user.setPermissions(permissions);
                userDAO.update(user);
            }
        });
        commands.put("giveflag", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 3);
                User user = userDAO.findByUsername(args[0]);
                if (user == null) {
                    LogHelper.error("User %s not found", args[0]);
                    return;
                }
                ClientPermissions permissions = user.getPermissions();
                long perm = Long.parseLong(args[1]);
                boolean value = Boolean.parseBoolean(args[2]);
                permissions.setFlag(perm, value);
                user.setPermissions(permissions);
                userDAO.update(user);
            }
        });
        return commands;
    }

    @Override
    public void close() {
        sessionFactory.close();
    }
}
