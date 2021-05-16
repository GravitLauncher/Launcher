package pro.gravit.launchserver.auth.handler;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.utils.ProviderMap;

import java.io.IOException;
import java.util.UUID;


public abstract class AuthHandler implements AutoCloseable {
    public static final ProviderMap<AuthHandler> providers = new ProviderMap<>("AuthHandler");
    private static boolean registredHandl = false;
    protected transient LaunchServer srv;

    public static UUID authError(String message) throws AuthException {
        throw new AuthException(message);
    }

    @SuppressWarnings("deprecation")
    public static void registerHandlers() {
        if (!registredHandl) {
            providers.register("null", NullAuthHandler.class);
            providers.register("json", JsonAuthHandler.class);
            providers.register("memory", MemoryAuthHandler.class);
            providers.register("mysql", MySQLAuthHandler.class);
            providers.register("postgresql", PostgreSQLAuthHandler.class);
            providers.register("request", RequestAuthHandler.class);
            providers.register("hibernate", HibernateAuthHandler.class);
            registredHandl = true;
        }
    }

    /**
     * Returns the UUID associated with the account
     *
     * @param authResult {@link pro.gravit.launchserver.auth.provider.AuthProvider} result
     * @return User UUID
     * @throws IOException Internal Script Error
     */
    public abstract UUID auth(AuthProviderResult authResult) throws IOException;

    /**
     * Validates serverID
     *
     * @param username user name
     * @param serverID serverID to check
     * @return user UUID
     * @throws IOException Internal Script Error
     */
    public abstract UUID checkServer(String username, String serverID) throws IOException;

    @Override
    public abstract void close() throws IOException;


    /**
     * Checks assessToken for validity and saves serverID if successful
     *
     * @param username    user name
     * @param accessToken assessToken to check
     * @param serverID    serverID to save
     * @return true - allow, false - deny
     * @throws IOException Internal Script Error
     */
    public abstract boolean joinServer(String username, String accessToken, String serverID) throws IOException;


    public abstract UUID usernameToUUID(String username) throws IOException;


    public abstract String uuidToUsername(UUID uuid) throws IOException;

    public void init(LaunchServer srv) {
        this.srv = srv;
    }
}
