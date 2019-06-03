package pro.gravit.launchserver.auth.handler;

import java.io.IOException;
import java.util.UUID;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.utils.ProviderMap;


public abstract class AuthHandler implements AutoCloseable {
    public static ProviderMap<AuthHandler> providers = new ProviderMap<>("AuthHandler");
    private static boolean registredHandl = false;


    public static UUID authError(String message) throws AuthException {
        throw new AuthException(message);
    }

    public static void registerHandlers() {
        if (!registredHandl) {
            providers.register("null", NullAuthHandler.class);
            providers.register("json", JsonAuthHandler.class);
            providers.register("memory", MemoryAuthHandler.class);
            providers.register("mysql", MySQLAuthHandler.class);
            providers.register("request", RequestAuthHandler.class);
            providers.register("hibernate", HibernateAuthHandler.class);
            registredHandl = true;
        }
    }

	protected transient LaunchServer srv;

    public abstract UUID auth(AuthProviderResult authResult) throws IOException;

    public abstract UUID checkServer(String username, String serverID) throws IOException;

    @Override
    public abstract void close() throws IOException;


    public abstract boolean joinServer(String username, String accessToken, String serverID) throws IOException;


    public abstract UUID usernameToUUID(String username) throws IOException;


    public abstract String uuidToUsername(UUID uuid) throws IOException;

    public void init(LaunchServer srv) {
    	this.srv = srv;
    }
}
