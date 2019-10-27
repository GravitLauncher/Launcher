package pro.gravit.launcher.api;

import pro.gravit.launcher.ClientPermissions;

import java.util.UUID;

public class AuthService {
    public static String username;
    public static ClientPermissions permissions = new ClientPermissions();
    public static UUID uuid;

    public static boolean isAdmin() {
        return permissions.canAdmin;
    }

    public static boolean isServer() {
        return permissions.canServer;
    }
}
