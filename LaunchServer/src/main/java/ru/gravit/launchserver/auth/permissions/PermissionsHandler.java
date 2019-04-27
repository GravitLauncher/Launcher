package ru.gravit.launchserver.auth.permissions;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.utils.ProviderMap;

public abstract class PermissionsHandler implements AutoCloseable {
    public static ProviderMap<PermissionsHandler> providers = new ProviderMap<>("PermissionsHandler");
    private static boolean registredHandl = false;

    public static void registerHandlers() {
        if (!registredHandl) {
            providers.register("json", JsonFilePermissionsHandler.class);
            providers.register("json-long", JsonLongFilePermissionsHandler.class);
            providers.register("config", ConfigPermissionsHandler.class);
            providers.register("default", DefaultPermissionsHandler.class);
            registredHandl = true;
        }
    }

    public abstract void init();

    public abstract ClientPermissions getPermissions(String username);

    public abstract void setPermissions(String username, ClientPermissions permissions);
}
