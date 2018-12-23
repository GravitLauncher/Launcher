package ru.gravit.launchserver.auth.provider;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AuthProvider implements AutoCloseable {
    private static final Map<String, Class> AUTH_PROVIDERS = new ConcurrentHashMap<>(8);
    private static boolean registredProv = false;
    private LaunchServer server;


    public static AuthProviderResult authError(String message) throws AuthException {
        throw new AuthException(message);
    }


    public static void registerProvider(String name, Class adapter) {
        VerifyHelper.putIfAbsent(AUTH_PROVIDERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Auth provider has been already registered: '%s'", name));
    }

    public static void registerProviders() {
        if (!registredProv) {
            registerProvider("null", NullAuthProvider.class);
            registerProvider("accept", AcceptAuthProvider.class);
            registerProvider("reject", RejectAuthProvider.class);

            // Auth providers that doesn't do nothing :D
            registerProvider("com.mojang", MojangAuthProvider.class);
            registerProvider("mysql", MySQLAuthProvider.class);
            registerProvider("request", RequestAuthProvider.class);
            registerProvider("json", JsonAuthProvider.class);
            registredProv = true;
        }
    }

    public AuthHandler getAccociateHandler(int this_position) {
        return server.config.authHandler[this_position];
    }


    public abstract AuthProviderResult auth(String login, String password, String ip) throws Exception;

    @Override
    public abstract void close() throws IOException;

    public static Class getProviderClass(String name)
    {
        return AUTH_PROVIDERS.get(name);
    }
    public static String getProviderName(Class clazz)
    {
        for(Map.Entry<String,Class> e: AUTH_PROVIDERS.entrySet())
        {
            if(e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }
}
