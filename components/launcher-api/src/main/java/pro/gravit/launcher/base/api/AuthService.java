package pro.gravit.launcher.base.api;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.profiles.ClientProfile;

import java.util.List;
import java.util.UUID;

public class AuthService {
    public static String projectName;
    public static String username;
    public static ClientPermissions permissions = new ClientPermissions();
    public static UUID uuid;
    public static ClientProfile profile;

    public static boolean hasPermission(String permission) {
        return permissions.hasPerm(permission);
    }

    public static boolean hasRole(String role) {
        return permissions.hasRole(role);
    }

    public static List<String> getRoles() {
        return permissions.getRoles();
    }
}
