package pro.gravit.launchserver.dao.provider;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.HwidDAO;
import pro.gravit.launchserver.dao.UserDAO;
import pro.gravit.utils.ProviderMap;

public abstract class DaoProvider {
    public static final ProviderMap<DaoProvider> providers = new ProviderMap<>("DaoProvider");
    public UserDAO userDAO;
    public HwidDAO hwidDao;

    public static void registerProviders() {
        providers.register("hibernate", HibernateDaoProvider.class);
    }

    public abstract void init(LaunchServer server);
}
