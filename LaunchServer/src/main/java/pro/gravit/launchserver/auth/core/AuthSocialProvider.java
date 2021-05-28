package pro.gravit.launchserver.auth.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.ProviderMap;

import java.io.IOException;
import java.util.List;

public abstract class AuthSocialProvider implements AutoCloseable {
    public static final ProviderMap<AuthSocialProvider> providers = new ProviderMap<>("AuthSocialProvider");
    private static final Logger logger = LogManager.getLogger();
    private static boolean registredProviders = false;

    public static void registerProviders() {
        if (!registredProviders) {
            registredProviders = true;
        }
    }

    public static class SocialResult {
        public String login;
        public AuthRequest.AuthPasswordInterface password;
        public User user;

        public SocialResult(String login, AuthRequest.AuthPasswordInterface password, User user) {
            this.login = login;
            this.password = password;
            this.user = user;
        }

        public static SocialResult ofLoginAndPassword(String login, AuthRequest.AuthPasswordInterface password) {
            return new SocialResult(login, password, null);
        }

        public static SocialResult ofUser(User user) {
            return new SocialResult(null, null, user);
        }
    }

    public abstract void init(LaunchServer server, AuthCoreProvider provider);

    public abstract List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client);

    public abstract SocialResult preAuth(AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password) throws AuthException;

    @Override
    public abstract void close() throws IOException;
}
