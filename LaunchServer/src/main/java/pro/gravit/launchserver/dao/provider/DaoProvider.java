package pro.gravit.launchserver.dao.provider;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.UserDAO;
import pro.gravit.utils.ProviderMap;

@Deprecated
public abstract class DaoProvider {
    public static final ProviderMap<DaoProvider> providers = new ProviderMap<>("DaoProvider");
    public transient UserDAO userDAO;

    public static void registerProviders() {
        // None
    }

    public abstract void init(LaunchServer server);
}
