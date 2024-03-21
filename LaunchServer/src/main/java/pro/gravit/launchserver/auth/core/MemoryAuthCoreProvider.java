package pro.gravit.launchserver.auth.core;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.details.AuthLoginOnlyDetails;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportSudo;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MemoryAuthCoreProvider extends AuthCoreProvider implements AuthSupportSudo {
    private transient final List<MemoryUser> memory = new ArrayList<>(16);

    @Override
    public User getUserByUsername(String username) {
        synchronized (memory) {
            for (MemoryUser u : memory) {
                if (u.username.equals(username)) {
                    return u;
                }
            }
            var result = new MemoryUser(username);
            memory.add(result);
            return result;
        }
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        return List.of(new AuthLoginOnlyDetails());
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        synchronized (memory) {
            for (MemoryUser u : memory) {
                if (u.uuid.equals(uuid)) {
                    return u;
                }
            }
        }
        return null;
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) {
        synchronized (memory) {
            for (MemoryUser u : memory) {
                if (u.accessToken.equals(accessToken)) {
                    return new MemoryUserSession(u);
                }
            }
        }
        return null;
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        return null;
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        if (login == null) {
            throw AuthException.userNotFound();
        }
        MemoryUser user = null;
        synchronized (memory) {
            for (MemoryUser u : memory) {
                if (u.username.equals(login)) {
                    user = u;
                    break;
                }
            }
            if (user == null) {
                user = new MemoryUser(login);
                memory.add(user);
            }
        }
        if (!minecraftAccess) {
            return AuthManager.AuthReport.ofOAuth(user.accessToken, null, 0, new MemoryUserSession(user));
        } else {
            return AuthManager.AuthReport.ofOAuthWithMinecraft(user.accessToken, user.accessToken, null, 0, new MemoryUserSession(user));
        }
    }

    @Override
    public User checkServer(Client client, String username, String serverID) {
        synchronized (memory) {
            for (MemoryUser u : memory) {
                if (u.username.equals(username)) {
                    return u;
                }
            }
            var result = new MemoryUser(username);
            memory.add(result);
            return result;
        }
    }

    @Override
    public boolean joinServer(Client client, String username, UUID uuid, String accessToken, String serverID) {
        return true;
    }

    @Override
    public void close() {

    }

    @Override
    public AuthManager.AuthReport sudo(User user, boolean shadow) throws IOException {
        return authorize(user.getUsername(), null, null, true);
    }

    public static class MemoryUser implements User {
        private final String username;
        private final UUID uuid;
        private String serverId;
        private final String accessToken;
        private final ClientPermissions permissions;

        public MemoryUser(String username) {
            this.username = username;
            this.uuid = makeUuidFromUsername(username);
            this.accessToken = SecurityHelper.randomStringToken();
            this.permissions = new ClientPermissions();
        }

        private static UUID makeUuidFromUsername(String username) {
            return UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public UUID getUUID() {
            return uuid;
        }

        @Override
        public ClientPermissions getPermissions() {
            return permissions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MemoryUser that = (MemoryUser) o;
            return uuid.equals(that.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }
    }

    public static class MemoryUserSession implements UserSession {
        private final String id;
        private final MemoryUser user;
        private final long expireIn;

        public MemoryUserSession(MemoryUser user) {
            this.id = SecurityHelper.randomStringToken();
            this.user = user;
            this.expireIn = 0;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public User getUser() {
            return user;
        }

        @Override
        public String getMinecraftAccessToken() {
            return "IGNORED";
        }

        @Override
        public long getExpireIn() {
            return expireIn;
        }
    }
}
