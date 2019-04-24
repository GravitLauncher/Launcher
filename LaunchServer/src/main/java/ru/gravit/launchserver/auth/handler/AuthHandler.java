package ru.gravit.launchserver.auth.handler;

import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.ProviderMap;

import java.io.IOException;
import java.util.UUID;

public abstract class AuthHandler implements AutoCloseable {
    public static ProviderMap<AuthHandler> providers = new ProviderMap<>("AuthHandler");
    private static boolean registredHandl = false;


    public static UUID authError(String message) throws AuthException {
        throw new AuthException(message);
    }

    public static void registerHandlers() {
        if (!registredHandl) {
            providers.registerProvider("null", NullAuthHandler.class);
            providers.registerProvider("json", JsonAuthHandler.class);
            providers.registerProvider("memory", MemoryAuthHandler.class);
            providers.registerProvider("mysql", MySQLAuthHandler.class);
            providers.registerProvider("request", RequestAuthHandler.class);
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

    public void init() {

    }
}
