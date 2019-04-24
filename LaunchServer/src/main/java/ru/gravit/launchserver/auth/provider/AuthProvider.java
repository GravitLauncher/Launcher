package ru.gravit.launchserver.auth.provider;

import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.utils.ProviderMap;

import java.io.IOException;

public abstract class AuthProvider implements AutoCloseable {
    public static ProviderMap<AuthProvider> providers = new ProviderMap<>("AuthProvider");
    private static boolean registredProv = false;

    public static AuthProviderResult authError(String message) throws AuthException {
        throw new AuthException(message);
    }

    public static void registerProviders() {
        if (!registredProv) {
            providers.registerProvider("null", NullAuthProvider.class);
            providers.registerProvider("accept", AcceptAuthProvider.class);
            providers.registerProvider("reject", RejectAuthProvider.class);
            providers.registerProvider("mysql", MySQLAuthProvider.class);
            providers.registerProvider("request", RequestAuthProvider.class);
            providers.registerProvider("json", JsonAuthProvider.class);
            registredProv = true;
        }
    }


    public abstract AuthProviderResult auth(String login, String password, String ip) throws Exception;

    public void preAuth(String login, String password, String customText, String ip) {
        return;
    }

    @Override
    public abstract void close() throws IOException;

    public void init() {

    }
}
