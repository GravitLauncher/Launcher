package pro.gravit.launchserver.auth.core;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthLoginOnlyDetails;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MemoryAuthCoreProvider extends AuthCoreProvider {
    private transient final List<MemoryUser> memory = new ArrayList<>(16);
    @Override
    public User getUserByUsername(String username) {
        synchronized (memory) {
            for(MemoryUser u : memory) {
                if(u.username.equals(username)) {
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
            for(MemoryUser u : memory) {
                if(u.uuid.equals(uuid)) {
                    return u;
                }
            }
        }
        return null;
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        synchronized (memory) {
            for(MemoryUser u : memory) {
                if(u.accessToken.equals(accessToken)) {
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
        if(login == null) {
            throw AuthException.userNotFound();
        }
        MemoryUser user = null;
        synchronized (memory) {
            for(MemoryUser u : memory) {
                if(u.username.equals(login)) {
                    user = u;
                    break;
                }
            }
            if(user == null) {
                user = new MemoryUser(login);
                memory.add(user);
            }
        }
        if(!minecraftAccess) {
            return AuthManager.AuthReport.ofOAuth(user.accessToken, null, 0, new MemoryUserSession(user));
        } else {
            return AuthManager.AuthReport.ofOAuthWithMinecraft(user.accessToken, user.accessToken, null, 0, new MemoryUserSession(user));
        }
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        MemoryUser memoryUser = (MemoryUser) user;
        memoryUser.serverId = serverID;
        return true;
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        synchronized (memory) {
            for(MemoryUser u : memory) {
                if(u.username.equals(username)) {
                    return u;
                }
            }
            var result = new MemoryUser(username);
            memory.add(result);
            return result;
        }
    }

    @Override
    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        return true;
    }

    @Override
    public void init(LaunchServer server) {

    }

    @Override
    public void close() throws IOException {

    }

    public static class MemoryUser implements User {
        private String username;
        private UUID uuid;
        private String serverId;
        private String accessToken;
        private ClientPermissions permissions;

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
        public String getServerId() {
            return serverId;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
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
        private String id;
        private MemoryUser user;
        private long expireIn;

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
        public long getExpireIn() {
            return expireIn;
        }
    }
}
