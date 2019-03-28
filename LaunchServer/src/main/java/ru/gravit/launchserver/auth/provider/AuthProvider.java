package ru.gravit.launchserver.auth.provider;

import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.utils.helper.VerifyHelper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AuthProvider implements AutoCloseable {
    private static final Map<String, Class<? extends AuthProvider>> AUTH_PROVIDERS = new ConcurrentHashMap<>(8);
    private static boolean registredProv = false;

    public static AuthProviderResult authError(String message) throws AuthException {
        throw new AuthException(message);
    }


    public static void registerProvider(String name, Class<? extends AuthProvider> adapter) {
        VerifyHelper.putIfAbsent(AUTH_PROVIDERS, name, Objects.requireNonNull(adapter, "adapter"),
                String.format("Auth provider has been already registered: '%s'", name));
    }

    public static void registerProviders() {
        if (!registredProv) {
            registerProvider("null", NullAuthProvider.class);
            registerProvider("accept", AcceptAuthProvider.class);
            registerProvider("reject", RejectAuthProvider.class);
            registerProvider("mysql", MySQLAuthProvider.class);
            registerProvider("request", RequestAuthProvider.class);
            registerProvider("json", JsonAuthProvider.class);
            registredProv = true;
        }
    }


    public abstract AuthProviderResult auth(String login, String password, String ip) throws Exception;

    public void preAuth(String login, String password, String customText, String ip) throws Exception
    {
        return;
    }

    @Override
    public abstract void close() throws IOException;

    public static Class<? extends AuthProvider> getProviderClass(String name) {
        return AUTH_PROVIDERS.get(name);
    }

    public static String getProviderName(Class<? extends AuthProvider> clazz) {
        for (Map.Entry<String, Class<? extends AuthProvider>> e : AUTH_PROVIDERS.entrySet()) {
            if (e.getValue().equals(clazz)) return e.getKey();
        }
        return null;
    }

    public void init() {

    }
}
