package pro.gravit.launchserver.auth.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class JsonCoreProvider extends AuthCoreProvider {
    private transient final Logger logger = LogManager.getLogger();
    public String getUserByUsernameUrl;
    public String getUserByLoginUrl;
    public String getUserByUUIDUrl;
    public String getUserSessionByOAuthAccessTokenUrl;
    public String getAuthDetailsUrl;
    public String refreshAccessTokenUrl;
    public String verifyPasswordUrl;
    public String createOAuthSessionUrl;
    public String updateServerIdUrl;
    public String bearerToken;
    public PasswordVerifier passwordVerifier;
    private transient HttpClient client;

    public static class JsonGetUserByUsername {
        public String username;

        public JsonGetUserByUsername(String username) {
            this.username = username;
        }
    }

    public static class JsonGetUserByUUID {
        public UUID uuid;

        public JsonGetUserByUUID(UUID uuid) {
            this.uuid = uuid;
        }
    }

    public static class JsonGetUserSessionByAccessToken {
        public String accessToken;

        public JsonGetUserSessionByAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    public static class JsonRefreshToken {
        public String refreshToken;
        public String ip;

        public JsonRefreshToken(String refreshToken, String ip) {
            this.refreshToken = refreshToken;
            this.ip = ip;
        }
    }

    public static class JsonAuthReportResponse {
        public String minecraftAccessToken;
        public String oauthAccessToken;
        public String oauthRefreshToken;
        public long oauthExpire;
        public JsonUserSession session;
        public String error;

        public AuthManager.AuthReport toAuthReport() {
            return new AuthManager.AuthReport(minecraftAccessToken, oauthAccessToken, oauthRefreshToken, oauthExpire, session);
        }
    }

    public static class JsonPasswordVerify {
        public String username;
        public UUID uuid;

        public JsonPasswordVerify(String username, UUID uuid) {
            this.username = username;
            this.uuid = uuid;
        }
    }

    public static class JsonCreateOAuthSession {
        public String username;
        public UUID uuid;
        public boolean minecraftAccess;

        public JsonCreateOAuthSession(String username, UUID uuid, boolean minecraftAccess) {
            this.username = username;
            this.uuid = uuid;
            this.minecraftAccess = minecraftAccess;
        }
    }

    public static class JsonUpdateServerId {
        public String username;
        public UUID uuid;
        public String serverId;

        public JsonUpdateServerId(String username, UUID uuid, String serverId) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
        }
    }

    public static class JsonSuccessResponse {
        public boolean success;
    }

    public static class JsonGetUserSessionByOAuthTokenResponse {
        public boolean expired;
        public JsonUserSession session;

        public JsonGetUserSessionByOAuthTokenResponse() {
        }
    }

    public static class JsonGetDetails {

    }

    public static class JsonGetDetailsResponse {
        public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> details;
    }

    @Override
    public User getUserByUsername(String username) {
        return jsonRequest(new JsonGetUserByUsername(username), getUserByUsernameUrl, JsonUser.class);
    }

    @Override
    public User getUserByLogin(String login) {
        if (getUserByLoginUrl != null) {
            return jsonRequest(new JsonGetUserByUsername(login), getUserByLoginUrl, JsonUser.class);
        }
        return super.getUserByLogin(login);
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        return jsonRequest(new JsonGetUserByUUID(uuid), getUserByUUIDUrl, JsonUser.class);
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        if (getUserSessionByOAuthAccessTokenUrl == null) {
            return null;
        }
        JsonGetUserSessionByOAuthTokenResponse response = jsonRequest(new JsonGetUserSessionByAccessToken(accessToken), getUserSessionByOAuthAccessTokenUrl, JsonGetUserSessionByOAuthTokenResponse.class);
        if (response == null) return null;
        if (!response.expired) throw new OAuthAccessTokenExpired();
        return response.session;
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        if (getAuthDetailsUrl != null) {
            JsonGetDetailsResponse response = jsonRequest(new JsonGetDetails(), getAuthDetailsUrl, JsonGetDetailsResponse.class);
            if (response == null) return super.getDetails(client);
            return response.details;
        }
        return super.getDetails(client);
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        JsonAuthReportResponse response = jsonRequest(new JsonRefreshToken(refreshToken, context.ip), this.refreshAccessTokenUrl, JsonAuthReportResponse.class);
        return response == null ? null : response.toAuthReport();
    }

    @Override
    public void verifyAuth(AuthResponse.AuthContext context) throws AuthException {

    }

    @Override
    public PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password) {
        JsonUser jsonUser = (JsonUser) user;
        if (password instanceof AuthPlainPassword && jsonUser.password != null && passwordVerifier != null) {
            if (passwordVerifier.check(((AuthPlainPassword) password).password, jsonUser.password)) {
                return PasswordVerifyReport.OK;
            } else {
                return PasswordVerifyReport.FAILED;
            }
        }
        if (user == null) {
            return jsonRequest(new JsonPasswordVerify(null, null), verifyPasswordUrl, PasswordVerifyReport.class);
        }
        return jsonRequest(new JsonPasswordVerify(user.getUsername(), user.getUUID()), verifyPasswordUrl, PasswordVerifyReport.class);
    }

    @Override
    public AuthManager.AuthReport createOAuthSession(User user, AuthResponse.AuthContext context, PasswordVerifyReport report, boolean minecraftAccess) throws IOException {
        JsonAuthReportResponse response = jsonRequest(new JsonCreateOAuthSession(user == null ? null : user.getUsername(), user == null ? null : user.getUUID(), minecraftAccess), createOAuthSessionUrl, JsonAuthReportResponse.class);
        if (response == null) return null;
        if (response.error != null) throw new AuthException(response.error);
        JsonUser user1 = (JsonUser) user;
        user1.accessToken = response.minecraftAccessToken;
        return response.toAuthReport();
    }

    @Override
    public void init(LaunchServer server) {
        client = HttpClient.newBuilder().build();
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        JsonSuccessResponse successResponse = jsonRequest(new JsonUpdateServerId(user.getUsername(), user.getUUID(), serverID), updateServerIdUrl, JsonSuccessResponse.class);
        if (successResponse == null) return false;
        return successResponse.success;
    }

    @Override
    public void close() throws IOException {

    }

    public static class JsonUser implements User {
        private String username;
        private UUID uuid;
        private String serverId;
        private String accessToken;
        private ClientPermissions permissions;
        private String password;

        public JsonUser() {
        }

        public JsonUser(String username, UUID uuid, String serverId, String accessToken, ClientPermissions permissions, String password) {
            this.username = username;
            this.uuid = uuid;
            this.serverId = serverId;
            this.accessToken = accessToken;
            this.permissions = permissions;
            this.password = password;
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
    }

    public static class JsonUserSession implements UserSession {
        public String id;
        public JsonUser user;
        public long expireIn;

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


    public <T, R> R jsonRequest(T request, String url, Class<R> clazz) {
        HttpRequest.BodyPublisher publisher;
        if (request != null) {
            publisher = HttpRequest.BodyPublishers.ofString(request.toString());
        } else {
            publisher = HttpRequest.BodyPublishers.noBody();
        }
        try {
            HttpRequest.Builder request1 = HttpRequest.newBuilder()
                    .method("POST", publisher)
                    .uri(new URI(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(10000));
            if (bearerToken != null) {
                request1.header("Authentication", "Bearer ".concat(bearerToken));
            }
            HttpResponse<InputStream> response = client.send(request1.build(), HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();
            if (200 > statusCode || statusCode > 300) {
                if (statusCode >= 500) {
                    logger.error("JsonCoreProvider: {} return {}", url, statusCode);
                } else if (statusCode >= 300 && statusCode <= 400) {
                    logger.error("JsonCoreProvider: {} return {}, try redirect to {}. Redirects not supported!", url, statusCode, response.headers().firstValue("Location").orElse("Unknown"));
                } else if (statusCode == 403 || statusCode == 401) {
                    logger.error("JsonCoreProvider: {} return {}. Please set 'bearerToken'!", url, statusCode);
                }
                return null;
            }
            try (Reader reader = new InputStreamReader(response.body())) {
                return Launcher.gsonManager.gson.fromJson(reader, clazz);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
