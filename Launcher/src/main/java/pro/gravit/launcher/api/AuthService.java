package pro.gravit.launcher.api;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.profiles.ClientProfile;

import java.util.UUID;

public class AuthService {
    public static String username;
    public static ClientPermissions permissions = new ClientPermissions();
    public static UUID uuid;
    public static ClientProfile profile;

    public static boolean hasPermission(String permission) {
        return permissions.hasPerm(permission);
    }
}
