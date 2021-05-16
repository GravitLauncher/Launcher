package pro.gravit.launchserver.auth.core;

import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthPasswordDetails;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/*
All-In-One provider
 */
public abstract class AuthCoreProvider implements AutoCloseable {
    public static final ProviderMap<AuthCoreProvider> providers = new ProviderMap<>("AuthCoreProvider");
    private static boolean registredProviders = false;
    public static void registerProviders() {
        if (!registredProviders) {
            registredProviders = true;
        }
    }
    public abstract User getUserByUsername(String username);
    public abstract User getUserByUUID(UUID uuid);
    public abstract void verifyAuth(AuthResponse.AuthContext context) throws AuthException;
    public abstract PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password);
    public abstract void init(LaunchServer server);
    // Auth Handler methods
    protected abstract boolean updateAuth(User user) throws IOException;
    protected abstract boolean updateServerID(User user, String serverID) throws IOException;

    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        return List.of(new AuthPasswordDetails());
    }

    public UUID checkServer(Client client, String username, String serverID) throws IOException {
        User user = getUserByUsername(username);
        if(user.getUsername().equals(username) && user.getServerId().equals(serverID)) {
            return user.getUUID();
        }
        return null;
    }

    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        User user = client.getUser();
        if(user == null) return false;
        return user.getUsername().equals(username) && user.getAccessToken().equals(accessToken) && updateServerID(user, serverID);
    }

    @Override
    public abstract void close() throws IOException;

    public static class PasswordVerifyReport {
        public final boolean success;
        public final boolean needMoreFactor;
        public final List<Integer> factors;
        public final String accessToken;
        public static final PasswordVerifyReport REQUIRED_2FA = new PasswordVerifyReport(-1);
        public static final PasswordVerifyReport FAILED = new PasswordVerifyReport(false);

        public PasswordVerifyReport(boolean success) {
            this.success = success;
            this.needMoreFactor = false;
            this.factors = List.of();
            this.accessToken = SecurityHelper.randomStringToken();
        }

        public PasswordVerifyReport(String accessToken) {
            this.success = true;
            this.needMoreFactor = false;
            this.factors = List.of();
            this.accessToken = accessToken;
        }

        public PasswordVerifyReport(int nextFactor) {
            this.success = false;
            this.needMoreFactor = true;
            this.factors = List.of(nextFactor);
            this.accessToken = null;
        }

        public PasswordVerifyReport(List<Integer> factors) {
            this.success = false;
            this.needMoreFactor = false;
            this.factors = Collections.unmodifiableList(factors);
            this.accessToken = null;
        }
    }
}
