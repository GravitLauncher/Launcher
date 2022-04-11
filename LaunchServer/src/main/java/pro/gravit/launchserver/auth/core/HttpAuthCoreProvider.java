package pro.gravit.launchserver.auth.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.HttpRequester;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportProperties;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportTextures;
import pro.gravit.launchserver.helper.HttpHelper;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.CommonHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HttpAuthCoreProvider extends AuthCoreProvider {
    private transient final Logger logger = LogManager.getLogger();
    private transient HttpRequester requester;
    public String bearerToken;
    public String getUserByUsernameUrl;
    public String getUserByLoginUrl;
    public String getUserByUUIDUrl;
    public String getUserByTokenUrl;
    public String getAuthDetails;
    public String refreshTokenUrl;
    public String authorizeUrl;
    public String joinServerUrl;
    public String checkServerUrl;
    public String updateServerIdUrl;
    @Override
    public User getUserByUsername(String username) {
        try {
            return requester.send(requester.get(CommonHelper.replace(getUserByUsernameUrl, "username", username), null), HttpUser.class).getOrThrow();
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public User getUserByLogin(String login) {
        if(getUserByLoginUrl != null) {
            try {
                return requester.send(requester.get(CommonHelper.replace(getUserByLoginUrl, "login", login), null), HttpUser.class).getOrThrow();
            } catch (IOException e) {
                logger.error(e);
                return null;
            }
        }
        return super.getUserByLogin(login);
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        try {
            return requester.send(requester.get(CommonHelper.replace(getUserByUUIDUrl, "uuid", uuid.toString()), null), HttpUser.class).getOrThrow();
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        if(getAuthDetails == null) {
            return super.getDetails(client);
        }
        try {
            var result = requester.send(requester.get(getAuthDetails, bearerToken), GetAuthDetailsResponse.class).getOrThrow();
            return result.details;
        } catch (IOException e) {
            logger.error(e);
            return super.getDetails(client);
        }
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        if(getUserByTokenUrl == null) {
            return null;
        }
        try {
            var result = requester.send(requester.get(getUserByTokenUrl, accessToken), HttpUserSession.class);
            if(!result.isSuccessful()) {
                var error = result.error().error;
                if(error.equals(AuthRequestEvent.OAUTH_TOKEN_EXPIRE)) {
                    throw new OAuthAccessTokenExpired();
                }
                return null;
            }
            return result.getOrThrow();
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        if(refreshTokenUrl == null) {
            return null;
        }
        try {
            return requester.send(requester.post(refreshTokenUrl, new RefreshTokenRequest(refreshToken, context),
                    null), AuthManager.AuthReport.class).getOrThrow();
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        var result = requester.send(requester.post(authorizeUrl, new AuthorizeRequest(login, context, password, minecraftAccess),
                bearerToken), HttpAuthReport.class);
        if(!result.isSuccessful()) {
            var error = result.error().error;
            if(error != null) {
                throw new AuthException(error);
            }
        }
        return result.getOrThrow().toAuthReport();
    }

    public record HttpAuthReport(String minecraftAccessToken, String oauthAccessToken,
                                 String oauthRefreshToken, long oauthExpire,
                                 HttpUserSession session) {
        public AuthManager.AuthReport toAuthReport() {
            return new AuthManager.AuthReport(minecraftAccessToken, oauthAccessToken, oauthRefreshToken, oauthExpire, session);
        }
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        var result = requester.send(requester.post(updateServerIdUrl, new UpdateServerIdRequest(user.getUsername(), user.getUUID(), serverID),
                null), Void.class);
        return result.isSuccessful();
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        return requester.send(requester.post(checkServerUrl, new CheckServerRequest(username, serverID), null), HttpUser.class).getOrThrow();
    }

    @Override
    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        var result = requester.send(requester.post(joinServerUrl, new JoinServerRequest(username, accessToken, serverID), null), Void.class);
        return result.isSuccessful();
    }

    public static class UpdateServerIdRequest {
        public String username;
        public UUID uuid;
        public String serverId;

        public UpdateServerIdRequest(String username, UUID uuid, String serverId) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
        }
    }

    public static class CheckServerRequest {
        public String username;
        public String serverId;

        public CheckServerRequest(String username, String serverId) {
            this.username = username;
            this.serverId = serverId;
        }
    }

    public static class GetAuthDetailsResponse {
        public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> details;
    }

    public static class JoinServerRequest {
        public String username;
        public String accessToken;
        public String serverId;

        public JoinServerRequest(String username, String accessToken, String serverId) {
            this.username = username;
            this.accessToken = accessToken;
            this.serverId = serverId;
        }
    }

    @Override
    public void init(LaunchServer server) {
        requester = new HttpRequester();
        if(getUserByUsernameUrl == null) {
            throw new IllegalArgumentException("'getUserByUsernameUrl' can't be null");
        }
        if(getUserByUUIDUrl == null) {
            throw new IllegalArgumentException("'getUserByUUIDUrl' can't be null");
        }
        if(authorizeUrl == null) {
            throw new IllegalArgumentException("'authorizeUrl' can't be null");
        }
        if(checkServerUrl == null && joinServerUrl == null && updateServerIdUrl == null) {
            throw new IllegalArgumentException("Please set 'checkServerUrl' and 'joinServerUrl' or 'updateServerIdUrl'");
        }
    }

    @Override
    public void close() throws IOException {

    }

    public static class AuthorizeRequest {
        public String login;
        public AuthResponse.AuthContext context;
        public AuthRequest.AuthPasswordInterface password;
        public boolean minecraftAccess;

        public AuthorizeRequest() {
        }

        public AuthorizeRequest(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) {
            this.login = login;
            this.context = context;
            this.password = password;
            this.minecraftAccess = minecraftAccess;
        }
    }

    public static class RefreshTokenRequest {
        public String refreshToken;
        public AuthResponse.AuthContext context;

        public RefreshTokenRequest(String refreshToken, AuthResponse.AuthContext context) {
            this.refreshToken = refreshToken;
            this.context = context;
        }
    }

    public static class HttpUser implements User, UserSupportTextures, UserSupportProperties {
        private String username;
        private UUID uuid;
        private String serverId;
        private String accessToken;
        private ClientPermissions permissions;
        @Deprecated
        private Texture skin;
        @Deprecated
        private Texture cloak;
        private Map<String, Texture> assets;
        private Map<String, String> properties;

        public HttpUser() {
        }

        public HttpUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
        }

        public HttpUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions, Texture skin, Texture cloak) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
            this.skin = skin;
            this.cloak = cloak;
        }

        public HttpUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions, Texture skin, Texture cloak, Map<String, String> properties) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
            this.skin = skin;
            this.cloak = cloak;
            this.properties = properties;
        }

        public HttpUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions, Map<String, Texture> assets, Map<String, String> properties) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
            this.assets = assets;
            this.properties = properties;
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
        public Texture getSkinTexture() {
            if(assets == null) {
                return skin;
            }
            return assets.get("SKIN");
        }

        @Override
        public Texture getCloakTexture() {
            if(assets == null) {
                return cloak;
            }
            return assets.get("CAPE");
        }

        public Map<String, Texture> getAssets() {
            if(assets == null) {
                Map<String, Texture> map = new HashMap<>();
                if(skin != null) {
                    map.put("SKIN", skin);
                }
                if(cloak != null) {
                    map.put("CAPE", cloak);
                }
                return map;
            }
            return assets;
        }

        @Override
        public Map<String, String> getProperties() {
            if(properties == null) {
                return new HashMap<>();
            }
            return properties;
        }

        @Override
        public String toString() {
            return "HttpUser{" +
                    "username='" + username + '\'' +
                    ", uuid=" + uuid +
                    ", serverId='" + serverId + '\'' +
                    ", accessToken='" + accessToken + '\'' +
                    ", permissions=" + permissions +
                    ", assets=" + getAssets() +
                    ", properties=" + properties +
                    '}';
        }
    }

    public static class HttpUserSession implements UserSession {
        private String id;
        private HttpUser user;
        private long expireIn;

        public HttpUserSession() {
        }

        public HttpUserSession(String id, HttpUser user, long expireIn) {
            this.id = id;
            this.user = user;
            this.expireIn = expireIn;
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

        @Override
        public String toString() {
            return "HttpUserSession{" +
                    "id='" + id + '\'' +
                    ", user=" + user +
                    ", expireIn=" + expireIn +
                    '}';
        }
    }
}
