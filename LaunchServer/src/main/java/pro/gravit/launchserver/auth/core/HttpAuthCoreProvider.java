package pro.gravit.launchserver.auth.core;

import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchserver.HttpRequester;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.interfaces.UserHardware;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportHardware;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportRemoteClientAccess;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportHardware;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportProperties;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportTextures;
import pro.gravit.launchserver.helper.HttpHelper;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.CommonHelper;

import java.io.IOException;
import java.util.*;

public class HttpAuthCoreProvider extends AuthCoreProvider implements AuthSupportHardware, AuthSupportRemoteClientAccess {
    private transient final Logger logger = LogManager.getLogger();
    public String bearerToken;
    public String getUserByUsernameUrl;
    public String getUserByLoginUrl;
    public String getUserByUUIDUrl;
    public String getUserByTokenUrl;
    public String getAuthDetailsUrl;
    public String refreshTokenUrl;
    public String authorizeUrl;
    public String joinServerUrl;
    public String checkServerUrl;
    public String updateServerIdUrl;
    //below fields can be empty if advanced protect handler disabled
    public String getHardwareInfoByPublicKeyUrl;
    public String getHardwareInfoByDataUrl;
    public String getHardwareInfoByIdUrl;
    public String createHardwareInfoUrl;
    public String connectUserAndHardwareUrl;
    public String addPublicKeyToHardwareInfoUrl;
    public String getUsersByHardwareInfoUrl;
    public String banHardwareUrl;
    public String unbanHardwareUrl;
    public String apiUrl;
    public List<String> apiFeatures;
    private transient HttpRequester requester;

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
        if (getUserByLoginUrl != null) {
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
        if (getAuthDetailsUrl == null) {
            return super.getDetails(client);
        }
        try {
            var result = requester.send(requester.get(getAuthDetailsUrl, bearerToken), GetAuthDetailsResponse.class).getOrThrow();
            return result.details;
        } catch (IOException e) {
            logger.error(e);
            return super.getDetails(client);
        }
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        if (getUserByTokenUrl == null) {
            return null;
        }
        try {
            var result = requester.send(requester.get(getUserByTokenUrl, accessToken), HttpUserSession.class);
            if (!result.isSuccessful()) {
                var error = result.error().error;
                if (error.equals(AuthRequestEvent.OAUTH_TOKEN_EXPIRE)) {
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
        if (refreshTokenUrl == null) {
            return null;
        }
        try {
            return requester.send(requester.post(refreshTokenUrl, new RefreshTokenRequest(refreshToken, context),
                    null), HttpAuthReport.class).getOrThrow().toAuthReport();
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        var result = requester.send(requester.post(authorizeUrl, new AuthorizeRequest(login, context, password, minecraftAccess),
                bearerToken), HttpAuthReport.class);
        if (!result.isSuccessful()) {
            var error = result.error().error;
            if (error != null) {
                throw new AuthException(error);
            }
        }
        return result.getOrThrow().toAuthReport();
    }

    @Override
    public UserHardware getHardwareInfoByPublicKey(byte[] publicKey) {
        if (getHardwareInfoByPublicKeyUrl == null) {
            return null;
        }
        try {
            return requester.send(requester.post(getHardwareInfoByPublicKeyUrl, new HardwareRequest(publicKey),
                    bearerToken), HttpUserHardware.class).getOrThrow();
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public UserHardware getHardwareInfoByData(HardwareReportRequest.HardwareInfo info) {
        if (getHardwareInfoByDataUrl == null) {
            return null;
        }
        try {
            HardwareRequest request = new HardwareRequest(new HttpUserHardware(info));
            HttpHelper.HttpOptional<HttpUserHardware, HttpRequester.SimpleError> hardware =
                    requester.send(requester.post(getHardwareInfoByDataUrl, request,
                            bearerToken), HttpUserHardware.class);
            //should return null if not found
            return hardware.isSuccessful() ? hardware.getOrThrow() : null;
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public UserHardware getHardwareInfoById(String id) {
        if (getHardwareInfoByIdUrl == null) {
            return null;
        }
        try {
            return requester.send(requester.post(getHardwareInfoByIdUrl, new HardwareRequest(new HttpUserHardware(Long.parseLong(id))),
                    bearerToken), HttpUserHardware.class).getOrThrow();
        } catch (IOException | NumberFormatException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public UserHardware createHardwareInfo(HardwareReportRequest.HardwareInfo info, byte[] publicKey) {
        if (createHardwareInfoUrl == null) {
            return null;
        }
        try {
            return requester.send(requester.post(createHardwareInfoUrl, new HardwareRequest(new HttpUserHardware(info,
                    publicKey, false)), bearerToken), HttpUserHardware.class).getOrThrow();
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public void connectUserAndHardware(UserSession userSession, UserHardware hardware) {
        if (connectUserAndHardwareUrl == null) {
            return;
        }
        try {
            requester.send(requester.post(connectUserAndHardwareUrl, new HardwareRequest((HttpUserHardware) hardware, (HttpUserSession) userSession), bearerToken), Void.class);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void addPublicKeyToHardwareInfo(UserHardware hardware, byte[] publicKey) {
        if (addPublicKeyToHardwareInfoUrl == null) {
            return;
        }
        try {
            requester.send(requester.post(addPublicKeyToHardwareInfoUrl, new HardwareRequest((HttpUserHardware) hardware, publicKey), bearerToken), Void.class);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Iterable<User> getUsersByHardwareInfo(UserHardware hardware) {
        if (getUsersByHardwareInfoUrl == null) {
            return null;
        }
        try {
            return (List<User>) (List) requester.send(requester
                    .post(getUsersByHardwareInfoUrl, new HardwareRequest((HttpUserHardware) hardware), bearerToken), GetHardwareListResponse.class).getOrThrow().list;
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public void banHardware(UserHardware hardware) {
        if (banHardwareUrl == null) {
            return;
        }
        try {
            requester.send(requester.post(banHardwareUrl, new HardwareRequest((HttpUserHardware) hardware), bearerToken), Void.class);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public void unbanHardware(UserHardware hardware) {
        if (unbanHardwareUrl == null) {
            return;
        }
        try {
            requester.send(requester.post(unbanHardwareUrl, new HardwareRequest((HttpUserHardware) hardware), bearerToken), Void.class);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public String getClientApiUrl() {
        return apiUrl;
    }

    @Override
    public List<String> getClientApiFeatures() {
        return apiFeatures;
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        var result = requester.send(requester.post(updateServerIdUrl, new UpdateServerIdRequest(user.getUsername(), user.getUUID(), serverID),
                null), Void.class);
        return result.isSuccessful();
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        return requester.send(requester.post(checkServerUrl, new CheckServerRequest(username, serverID), bearerToken), HttpUser.class).getOrThrow();
    }

    @Override
    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        var result = requester.send(requester.post(joinServerUrl, new JoinServerRequest(username, accessToken, serverID), bearerToken), Void.class);
        return result.isSuccessful();
    }

    @Override
    public void init(LaunchServer server) {
        requester = new HttpRequester();
        if (getUserByUsernameUrl == null) {
            throw new IllegalArgumentException("'getUserByUsernameUrl' can't be null");
        }
        if (getUserByUUIDUrl == null) {
            throw new IllegalArgumentException("'getUserByUUIDUrl' can't be null");
        }
        if (authorizeUrl == null) {
            throw new IllegalArgumentException("'authorizeUrl' can't be null");
        }
        if (checkServerUrl == null && joinServerUrl == null && updateServerIdUrl == null) {
            throw new IllegalArgumentException("Please set 'checkServerUrl' and 'joinServerUrl' or 'updateServerIdUrl'");
        }
    }

    @Override
    public void close() throws IOException {

    }

    public record HttpAuthReport(String minecraftAccessToken, String oauthAccessToken,
                                 String oauthRefreshToken, long oauthExpire,
                                 HttpUserSession session) {
        public AuthManager.AuthReport toAuthReport() {
            return new AuthManager.AuthReport(minecraftAccessToken, oauthAccessToken, oauthRefreshToken, oauthExpire, session);
        }
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

    public static class GetHardwareListResponse {
        public List<HttpUser> list;
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

    public record HardwareRequest(HttpUserHardware userHardware, byte[] key, HttpUserSession userSession) {

        public HardwareRequest(HttpUserHardware userHardware) {
            this(userHardware, null, null);
        }

        public HardwareRequest(HttpUserHardware userHardware, byte[] key) {
            this(userHardware, key, null);
        }

        public HardwareRequest(HttpUserHardware userHardware, HttpUserSession userSession) {
            this(userHardware, null, userSession);
        }

        public HardwareRequest(byte[] key) {
            this(null, key, null);
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

    public static class HttpUserHardware implements UserHardware {
        private final HardwareReportRequest.HardwareInfo hardwareInfo;
        private final long id;
        private byte[] publicKey;
        private boolean banned;

        public HttpUserHardware(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, long id, boolean banned) {
            this.hardwareInfo = hardwareInfo;
            this.publicKey = publicKey;
            this.id = id;
            this.banned = banned;
        }

        public HttpUserHardware(HardwareReportRequest.HardwareInfo hardwareInfo) {
            this.hardwareInfo = hardwareInfo;
            this.id = Long.MIN_VALUE;
        }

        public HttpUserHardware(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, boolean banned) {
            this.hardwareInfo = hardwareInfo;
            this.publicKey = publicKey;
            this.banned = banned;
            this.id = Long.MIN_VALUE;
        }

        public HttpUserHardware(long id) {
            this.id = id;
            this.hardwareInfo = null;
        }

        @Override
        public HardwareReportRequest.HardwareInfo getHardwareInfo() {
            return hardwareInfo;
        }

        @Override
        public byte[] getPublicKey() {
            return publicKey;
        }

        @Override
        public String getId() {
            return String.valueOf(id);
        }

        @Override
        public boolean isBanned() {
            return banned;
        }

        @Override
        public String toString() {
            return "HttpUserHardware{" +
                    "hardwareInfo=" + hardwareInfo +
                    ", publicKey=" + (publicKey == null ? null : new String(Base64.getEncoder().encode(publicKey))) +
                    ", id=" + id +
                    ", banned=" + banned +
                    '}';
        }
    }

    public class HttpUser implements User, UserSupportTextures, UserSupportProperties, UserSupportHardware {
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
        private long hwidId;
        private transient HttpUserHardware hardware;

        public HttpUser() {
        }

        public HttpUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions, long hwidId) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
            this.hwidId = hwidId;
        }

        public HttpUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions, Texture skin, Texture cloak, long hwidId) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
            this.skin = skin;
            this.cloak = cloak;
            this.hwidId = hwidId;
        }

        public HttpUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions, Texture skin, Texture cloak, Map<String, String> properties, long hwidId) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
            this.skin = skin;
            this.cloak = cloak;
            this.properties = properties;
            this.hwidId = hwidId;
        }

        public HttpUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions, Map<String, Texture> assets, Map<String, String> properties, long hwidId) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
            this.assets = assets;
            this.properties = properties;
            this.hwidId = hwidId;
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
            if (assets == null) {
                return skin;
            }
            return assets.get("SKIN");
        }

        @Override
        public Texture getCloakTexture() {
            if (assets == null) {
                return cloak;
            }
            return assets.get("CAPE");
        }

        public Map<String, Texture> getAssets() {
            if (assets == null) {
                Map<String, Texture> map = new HashMap<>();
                if (skin != null) {
                    map.put("SKIN", skin);
                }
                if (cloak != null) {
                    map.put("CAPE", cloak);
                }
                return map;
            }
            return assets;
        }

        @Override
        public Map<String, String> getProperties() {
            if (properties == null) {
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
                    ", hwidId=" + hwidId +
                    '}';
        }

        @Override
        public UserHardware getHardware() {
            if (hardware != null) return hardware;
            HttpAuthCoreProvider.HttpUserHardware result = (HttpUserHardware) getHardwareInfoById(String.valueOf(hwidId));
            hardware = result;
            return result;
        }
    }
}
