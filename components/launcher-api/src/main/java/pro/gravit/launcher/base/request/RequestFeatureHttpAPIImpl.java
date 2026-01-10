package pro.gravit.launcher.base.request;

import com.google.gson.JsonElement;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.helper.HttpHelper;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.request.auth.password.AuthPlainPassword;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.api.features.UserFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.method.password.AuthChainPassword;
import pro.gravit.launcher.core.api.method.password.AuthTotpPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.User;
import pro.gravit.launcher.core.api.model.UserPermissions;
import pro.gravit.launcher.core.hasher.HashedDir;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class RequestFeatureHttpAPIImpl implements AuthFeatureAPI, UserFeatureAPI, ProfileFeatureAPI {
    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().build();
    private AtomicReference<HttpAuthData> authDataRef = new AtomicReference<>();
    private AtomicReference<ClientProfile> profileRef = new AtomicReference<>();

    public RequestFeatureHttpAPIImpl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public CompletableFuture<SelfUser> getCurrentUser() {
        try {
            var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
            if(accessToken.isEmpty()) {
                return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
            }
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create(baseUrl.concat("/auth/currentuser")))
                            .header("Authorization", "Bearer "+accessToken.get())
                    .build(), new HttpErrorHandler<>(HttpUser.class));
            if(result.isSuccessful()) {
                var res = result.result();
                HttpSelfUser httpSelfUser = new HttpSelfUser();
                httpSelfUser.username = res.getUsername();
                httpSelfUser.uuid = res.getUUID();
                httpSelfUser.assets = res.getAssets();
                httpSelfUser.properties = res.getProperties();
                return CompletableFuture.completedFuture(httpSelfUser);
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<AuthResponse> auth(String login, AuthMethodPassword password) {
        try {
            String rawPassword;
            String rawTotp;
            if(password instanceof AuthPlainPassword plain) {
                rawPassword = plain.password;
                rawTotp = null;
            } else if(password instanceof AuthChainPassword chain) {
                rawPassword = null;
                rawTotp = null;
                for(var e : chain.list()) {
                    if(e instanceof AuthPlainPassword plain) {
                        rawPassword = plain.password;
                    } else if(e instanceof AuthTotpPassword(String value)) {
                        rawTotp = value;
                    }
                }
            } else {
                return CompletableFuture.failedFuture(new RequestException("Unknown password type"));
            }
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                            .POST(HttpHelper.jsonBodyPublisher(new HttpAuthRequest(login, rawPassword, rawTotp)))
                    .uri(URI.create(baseUrl.concat("/auth/authorize")))
                    .build(), new HttpErrorHandler<>(HttpAuthData.class));
            if(result.isSuccessful()) {
                authDataRef.set(result.result());
                return getCurrentUser().thenApply((selfUser) -> {
                    return new AuthResponse(selfUser, result.result());
                });
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<AuthToken> refreshToken(String refreshToken) {
        try {
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .POST(HttpHelper.jsonBodyPublisher(new HttpRefreshRequest(refreshToken)))
                    .uri(URI.create(baseUrl.concat("/auth/refresh")))
                    .build(), new HttpErrorHandler<>(HttpAuthData.class));
            if(result.isSuccessful()) {
                return CompletableFuture.completedFuture(result.result());
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<SelfUser> restore(String accessToken, boolean fetchUser) {
        authDataRef.set(new HttpAuthData(accessToken, null, 0));
        return getCurrentUser();
    }

    @Override
    public CompletableFuture<Void> exit() {
        try {
            var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
            if(accessToken.isEmpty()) {
                return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
            }
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .uri(URI.create(baseUrl.concat("/auth/exit")))
                    .header("Authorization", "Bearer "+accessToken.get())
                    .build(), new HttpErrorHandler<>(Void.class));
            if(result.isSuccessful()) {
                return CompletableFuture.completedFuture(result.result());
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<User> getUserByUsername(String username) {
        try {
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(baseUrl.concat("/user/by/username/").concat(URLEncoder.encode(username, StandardCharsets.UTF_8))))
                    .build(), new HttpErrorHandler<>(HttpUser.class));
            if(result.isSuccessful()) {
                return CompletableFuture.completedFuture(result.result());
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<User> getUserByUUID(UUID uuid) {
        try {
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(baseUrl.concat("/user/by/uuid/").concat(uuid.toString())))
                    .build(), new HttpErrorHandler<>(HttpUser.class));
            if(result.isSuccessful()) {
                return CompletableFuture.completedFuture(result.result());
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> joinServer(String username, String accessToken, String serverID) {
        try {
            var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
            if(accessToken0.isEmpty()) {
                return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
            }
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .POST(HttpHelper.jsonBodyPublisher(new HttpJoinServerByUsernameRequest(username, serverID, accessToken)))
                    .uri(URI.create(baseUrl.concat("/auth/joinserver/username")))
                    .header("Authorization", "Bearer "+accessToken0.get())
                    .build(), new HttpErrorHandler<>(HttpCheckServerResponse.class));
            if(result.isSuccessful()) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> joinServer(UUID uuid, String accessToken, String serverID) {
        try {
            var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
            if(accessToken0.isEmpty()) {
                return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
            }
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .POST(HttpHelper.jsonBodyPublisher(new HttpJoinServerByUuidRequest(uuid.toString(), serverID, accessToken)))
                    .uri(URI.create(baseUrl.concat("/auth/joinserver/uuid")))
                    .header("Authorization", "Bearer "+accessToken0.get())
                    .build(), new HttpErrorHandler<>(HttpCheckServerResponse.class));
            if(result.isSuccessful()) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<CheckServerResponse> checkServer(String username, String serverID, boolean extended) {
        try {
            var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
            if(accessToken.isEmpty()) {
                return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
            }
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .POST(HttpHelper.jsonBodyPublisher(new HttpCheckServerRequest(username, serverID, extended)))
                    .uri(URI.create(baseUrl.concat("/auth/checkserver")))
                    .header("Authorization", "Bearer "+accessToken.get())
                    .build(), new HttpErrorHandler<>(HttpCheckServerResponse.class));
            if(result.isSuccessful()) {
                return CompletableFuture.completedFuture(result.result().toDefaultResult());
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<List<ClientProfile>> getProfiles() {
        try {
            var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
            if(accessToken0.isEmpty()) {
                return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
            }
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(baseUrl.concat("/profile/list")))
                    .header("Authorization", "Bearer "+accessToken0.get())
                    .build(), new HttpErrorHandler<>(HttpListProfilesResponse.class));
            if(result.isSuccessful()) {
                List<ClientProfile> profiles = new ArrayList<>(result.result().profiles());
                return CompletableFuture.completedFuture(profiles);
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<ClientProfile> changeCurrentProfile(ClientProfile profile) {
        profileRef.set(profile);
        return CompletableFuture.completedFuture(profile);
    }

    @Override
    public CompletableFuture<UpdateInfo> fetchUpdateInfo(String dirName) {
        try {
            var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
            if(accessToken0.isEmpty()) {
                return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
            }
            var result = HttpHelper.send(client, HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(baseUrl.concat(String.format("/profile/%s/dir/%s", profileRef.get().getUUID(), dirName))))
                    .header("Authorization", "Bearer "+accessToken0.get())
                    .build(), new HttpErrorHandler<>(HttpUpdateInfo.class));
            if(result.isSuccessful()) {
                return CompletableFuture.completedFuture(result.result());
            }
            return CompletableFuture.failedFuture(new RequestException(result.error().toString()));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public static class HttpUser implements User {

        public String username;
        public UUID uuid;
        public Map<String, Texture> assets;
        public Map<String, String> properties;

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public UUID getUUID() {
            return uuid;
        }

        @Override
        public Map<String, Texture> getAssets() {
            return assets;
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }
    }

    public static class HttpSelfUser extends HttpUser implements SelfUser {



        @Override
        public String getAccessToken() {
            return "";
        }

        @Override
        public UserPermissions getPermissions() {
            return null;
        }
    }

    public static class HttpAuthData implements AuthToken {
        public String accessToken;
        public String refreshToken;
        public long expireSeconds;

        public HttpAuthData() {
        }

        public HttpAuthData(String accessToken, String refreshToken, long expireSeconds) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expireSeconds = expireSeconds;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public String getRefreshToken() {
            return refreshToken;
        }

        @Override
        public long getExpire() {
            return expireSeconds;
        }
    }

    private static class HttpErrorHandler<T> implements HttpHelper.HttpJsonErrorHandler<T, ErrorResponse> {
        private final Class<T> type;

        public HttpErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, ErrorResponse> applyJson(JsonElement response, int statusCode) {
            if(statusCode >= 300 || statusCode < 200) {
                return new HttpHelper.HttpOptional<>(null, Launcher.gsonManager.gson.fromJson(response, ErrorResponse.class), statusCode);
            }
            return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(response, type), null, statusCode);
        }
    }

    public record ErrorResponse(String code, String error) {
    }

    public record HttpAuthRequest(String login, String password, String totp) {

    }

    public record HttpCheckServerRequest(String username, String serverId, boolean extended) {

    }

    public record HttpJoinServerByUsernameRequest(String username, String serverId, String accessToken) {

    }

    public record HttpListProfilesResponse(List<pro.gravit.launcher.base.profiles.ClientProfile> profiles) {

    }

    public record HttpJoinServerByUuidRequest(String uuid, String serverId, String accessToken) {

    }

    public record HttpUpdateInfo(HashedDir dir, String baseUrl) implements UpdateInfo {

        @Override
        public HashedDir getHashedDir() {
            return dir;
        }

        @Override
        public String getUrl() {
            return baseUrl;
        }
    }

    public record HttpRefreshRequest(String refreshToken) {

    }

    record HttpCheckServerResponse(HttpUser user, String hardwareId, String sessionId, Map<String, String> sessionProperties) {
        CheckServerResponse toDefaultResult() {
            return new CheckServerResponse(user, hardwareId, sessionId, sessionProperties);
        }
    }
}
