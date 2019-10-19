package pro.gravit.launchserver.auth.permissions;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.ProviderMap;

public abstract class PermissionsHandler implements AutoCloseable {
    public static final ProviderMap<PermissionsHandler> providers = new ProviderMap<>("PermissionsHandler");
    protected transient LaunchServer srv;
    private static boolean registredHandl = false;

    public static void registerHandlers() {
        if (!registredHandl) {
            providers.register("json", JsonFilePermissionsHandler.class);
            providers.register("json-long", JsonLongFilePermissionsHandler.class);
            providers.register("config", ConfigPermissionsHandler.class);
            providers.register("default", DefaultPermissionsHandler.class);
            providers.register("hibernate", HibernatePermissionsHandler.class);
            registredHandl = true;
        }
    }

    public void init(LaunchServer server)
    {
        this.srv = server;
    }

    public abstract ClientPermissions getPermissions(String username);

    public abstract void setPermissions(String username, ClientPermissions permissions);
}
