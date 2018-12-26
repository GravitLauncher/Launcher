package ru.gravit.launchserver.auth.handler;

import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AuthHandler implements AutoCloseable {
    private static final Map<String, Class> AUTH_HANDLERS = new ConcurrentHashMap<>(4);
    private static boolean registredHandl = false;


    public static UUID authError(String message) throws AuthException {
        throw new AuthException(message);
    }


    public static void registerHandler(String name, Class adapter) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(AUTH_HANDLERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Auth handler has been already registered: '%s'", name));
    }
    public static Class getHandlerClass(String name)
    {
        return AUTH_HANDLERS.get(name);
    }
    public static String getHandlerName(Class clazz)
    {
        for(Map.Entry<String,Class> e: AUTH_HANDLERS.entrySet())
        {
            if(e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }

    public static void registerHandlers() {
        if (!registredHandl) {
            registerHandler("null", NullAuthHandler.class);
            registerHandler("memory", MemoryAuthHandler.class);

            // Auth handler that doesn't do nothing :D
            registerHandler("binaryFile", BinaryFileAuthHandler.class);
            registerHandler("mysql", MySQLAuthHandler.class);
            registredHandl = true;
        }
    }

    public abstract UUID auth(AuthProviderResult authResult) throws IOException;

    public abstract UUID checkServer(String username, String serverID) throws IOException;

    @Override
    public abstract void close() throws IOException;


    public abstract boolean joinServer(String username, String accessToken, String serverID) throws IOException;


    public abstract UUID usernameToUUID(String username) throws IOException;


    public abstract String uuidToUsername(UUID uuid) throws IOException;

    public void init()
    {

    }
}
