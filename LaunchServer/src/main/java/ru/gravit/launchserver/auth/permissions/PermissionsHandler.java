package ru.gravit.launchserver.auth.permissions;

import ru.gravit.launchserver.auth.ClientPermissions;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PermissionsHandler {
    private static final Map<String, Class> PERMISSIONS_HANDLERS = new ConcurrentHashMap<>(4);
    private static boolean registredHandl = false;
    public static void registerHandler(String name, Class adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(PERMISSIONS_HANDLERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Auth handler has been already registered: '%s'", name));
    }
    public static Class getHandlerClass(String name)
    {
        return PERMISSIONS_HANDLERS.get(name);
    }
    public static String getHandlerName(Class clazz)
    {
        for(Map.Entry<String,Class> e: PERMISSIONS_HANDLERS.entrySet())
        {
            if(e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }
    public static void registerHandlers() {
        if (!registredHandl) {
            registerHandler("json", JsonFilePermissionsHandler.class);
            registerHandler("config", ConfigPermissionsHandler.class);
            registerHandler("default", DefaultPermissionsHandler.class);
            registredHandl = true;
        }
    }
    public abstract ClientPermissions getPermissions(String username);
}
