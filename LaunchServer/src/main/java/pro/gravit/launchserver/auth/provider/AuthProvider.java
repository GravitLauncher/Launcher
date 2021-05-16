package pro.gravit.launchserver.auth.provider;

import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthPasswordDetails;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.ProviderMap;

import java.io.IOException;
import java.util.List;

public abstract class AuthProvider implements AutoCloseable {
    public static final ProviderMap<AuthProvider> providers = new ProviderMap<>("AuthProvider");
    private static boolean registredProv = false;
    protected transient LaunchServer srv = null;

    public static AuthProviderResult authError(String message) throws AuthException {
        throw new AuthException(message);
    }

    @SuppressWarnings("deprecation")
    public static void registerProviders() {
        if (!registredProv) {
            providers.register("null", NullAuthProvider.class);
            providers.register("accept", AcceptAuthProvider.class);
            providers.register("reject", RejectAuthProvider.class);
            providers.register("mysql", MySQLAuthProvider.class);
            providers.register("postgresql", PostgreSQLAuthProvider.class);
            providers.register("request", RequestAuthProvider.class);
            providers.register("json", JsonAuthProvider.class);
            providers.register("hibernate", HibernateAuthProvider.class);
            registredProv = true;
        }
    }

    @Deprecated
    public GetAvailabilityAuthRequestEvent.AuthAvailability.AuthType getFirstAuthType() {
        return GetAvailabilityAuthRequestEvent.AuthAvailability.AuthType.PASSWORD;
    }

    @Deprecated
    public GetAvailabilityAuthRequestEvent.AuthAvailability.AuthType getSecondAuthType() {
        return GetAvailabilityAuthRequestEvent.AuthAvailability.AuthType.NONE;
    }

    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        return List.of(new AuthPasswordDetails());
    }

    /**
     * Verifies the username and password
     *
     * @param login    user login
     * @param password user password
     * @param ip       user ip
     * @return player privileges, effective username and authorization token
     * @throws Exception Throws an exception {@link AuthException} {@link pro.gravit.utils.HookException} if the verification script returned a meaningful error
     *                   In other cases, throwing an exception indicates a serious error
     */
    public abstract AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws Exception;

    public void preAuth(String login, AuthRequest.AuthPasswordInterface password, String ip) {
    }

    @Override
    public abstract void close() throws IOException;

    public void init(LaunchServer srv) {
        this.srv = srv;
    }
}
