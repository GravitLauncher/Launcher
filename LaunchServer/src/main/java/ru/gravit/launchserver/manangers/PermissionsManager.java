package ru.gravit.launchserver.manangers;

import ru.gravit.launchserver.auth.ClientPermissions;

public class PermissionsManager {
    private static PermissionsFunction function = PermissionsManager::returnDefault;
    public static void registerPermissionsFunction(PermissionsFunction function)
    {
        PermissionsManager.function = function;
    }
    public static ClientPermissions getPermissions(String username)
    {
        return function.getPermission(username);
    }
    @FunctionalInterface
    public interface PermissionsFunction
    {
        ClientPermissions getPermission(String username);
    }
    public static ClientPermissions returnDefault(String username)
    {
        return ClientPermissions.DEFAULT;
    }
}
