package pro.gravit.launcher.base.request;

import com.google.gson.JsonElement;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.helper.HttpHelper;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.core.api.features.*;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodDetails;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.method.details.AuthPasswordDetails;
import pro.gravit.launcher.core.api.method.details.AuthTotpDetails;
import pro.gravit.launcher.core.api.method.password.AuthChainPassword;
import pro.gravit.launcher.core.api.method.password.AuthPlainPassword;
import pro.gravit.launcher.core.api.method.password.AuthTotpPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.User;
import pro.gravit.launcher.core.api.model.UserPermissions;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class RequestFeatureHttpAPIImpl implements AuthFeatureAPI, UserFeatureAPI, ProfileFeatureAPI, CoreFeatureAPI, HardwareVerificationFeatureAPI {
    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().build();
    private AtomicReference<HttpAuthData> authDataRef = new AtomicReference<>();
    private AtomicReference<ClientProfile> profileRef = new AtomicReference<>();

    public RequestFeatureHttpAPIImpl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public CompletableFuture<SelfUser> getCurrentUser() {
        var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
        if(accessToken.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized (currentuser)"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl.concat("/auth/currentuser")))
                .header("Authorization", "Bearer "+accessToken.get())
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpUser.class)).thenApply(result -> {
            var res = result.result();
            HttpSelfUser httpSelfUser = new HttpSelfUser();
            httpSelfUser.username = res.getUsername();
            httpSelfUser.uuid = res.getUUID();
            httpSelfUser.assets = (Map) res.getAssets();
            httpSelfUser.properties = res.getProperties();
            return httpSelfUser;
        });
    }

    @Override
    public CompletableFuture<AuthResponse> auth(String login, AuthMethodPassword password) {
        String rawPassword;
        String rawTotp;
        if (password instanceof AuthPlainPassword plain) {
            rawPassword = plain.value();
            rawTotp = null;
        } else if (password instanceof AuthChainPassword(List<AuthMethodPassword> list)) {
            rawPassword = null;
            rawTotp = null;
            for (var e : list) {
                if (e instanceof AuthPlainPassword plain) {
                    rawPassword = plain.value();
                } else if (e instanceof AuthTotpPassword(String value)) {
                    rawTotp = value;
                }
            }
        } else {
            return CompletableFuture.failedFuture(new RequestException("Unknown password type"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .POST(HttpHelper.jsonBodyPublisher(new HttpAuthRequest(login, rawPassword, rawTotp)))
                .uri(URI.create(baseUrl.concat("/auth/authorize")))
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpAuthData.class)).thenCompose(result -> {
            authDataRef.set(result.getOrThrow());
            return getCurrentUser().thenApply((selfUser) -> new AuthResponse(selfUser, result.result()));
        });
    }

    @Override
    public CompletableFuture<AuthToken> refreshToken(String refreshToken) {
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .POST(HttpHelper.jsonBodyPublisher(new HttpRefreshRequest(refreshToken)))
                .uri(URI.create(baseUrl.concat("/auth/refresh")))
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpAuthData.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    @Override
    public CompletableFuture<SelfUser> restore(String accessToken, boolean fetchUser) {
        authDataRef.set(new HttpAuthData(accessToken, null, 0));
        return getCurrentUser();
    }

    @Override
    public CompletableFuture<Void> exit() {
        var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
        if(accessToken.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(baseUrl.concat("/auth/logout")))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer "+accessToken.get())
                .build(), new HttpErrorHandler<>(Void.class)).thenApply(HttpHelper.HttpOptional::getOrThrow).thenApply(e -> {
                    authDataRef.set(null);
                    return e;
        });
    }

    @Override
    public CompletableFuture<User> getUserByUsername(String username) {
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl.concat("/user/by/username/").concat(URLEncoder.encode(username, StandardCharsets.UTF_8))))
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpUser.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    @Override
    public CompletableFuture<User> getUserByUUID(UUID uuid) {
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl.concat("/user/by/uuid/").concat(uuid.toString())))
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpUser.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    @Override
    public CompletableFuture<Void> joinServer(String username, String accessToken, String serverID) {
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .POST(HttpHelper.jsonBodyPublisher(new HttpJoinServerByUsernameRequest(username, serverID, accessToken)))
                .uri(URI.create(baseUrl.concat("/auth/joinserver/username")))
                .header("Authorization", "Bearer "+accessToken0.get())
                        .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpCheckServerResponse.class))
                .thenApply(HttpHelper.HttpOptional::getOrThrow).thenApply(e -> null);
    }

    @Override
    public CompletableFuture<Void> joinServer(UUID uuid, String accessToken, String serverID) {
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .POST(HttpHelper.jsonBodyPublisher(new HttpJoinServerByUuidRequest(uuid.toString(), serverID, accessToken)))
                .uri(URI.create(baseUrl.concat("/auth/joinserver/uuid")))
                .header("Authorization", "Bearer "+accessToken0.get())
                        .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpCheckServerResponse.class))
                .thenApply(HttpHelper.HttpOptional::getOrThrow).thenApply(e -> null);
    }

    @Override
    public CompletableFuture<CheckServerResponse> checkServer(String username, String serverID, boolean extended) {
        var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
        if(accessToken.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .POST(HttpHelper.jsonBodyPublisher(new HttpCheckServerRequest(username, serverID, extended)))
                .uri(URI.create(baseUrl.concat("/auth/checkserver")))
                .header("Authorization", "Bearer "+accessToken.get())
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpCheckServerResponse.class))
                .thenApply(e -> e.getOrThrow().toDefaultResult());
    }

    @Override
    public CompletableFuture<List<ClientProfile>> getProfiles() {
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl.concat("/profile/list")))
                .header("Authorization", "Bearer "+accessToken0.get())
                        .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpListProfilesResponse.class))
                .thenApply(e -> new ArrayList<>(e.getOrThrow().profiles()));
    }

    @Override
    public CompletableFuture<ClientProfile> changeCurrentProfile(ClientProfile profile) {
        profileRef.set(profile);
        return CompletableFuture.completedFuture(profile);
    }

    @Override
    public CompletableFuture<UpdateInfo> fetchUpdateInfo(String dirName) {
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.accessToken);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl.concat(String.format("/profile/%s/dir/%s", profileRef.get().getUUID(), dirName))))
                .header("Authorization", "Bearer "+accessToken0.get())
                        .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpUpdateInfo.class))
                .thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    @Override
    public CompletableFuture<List<AuthMethod>> getAuthMethods() {
        return CompletableFuture.completedFuture(List.of(new HttoAuthMethod()));
    }

    @Override
    public CompletableFuture<LauncherUpdateInfo> checkUpdates() {
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl.concat("/updates/prepare")))
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpUpdatesPrepare.class)).thenCompose(result -> {
                    var res = result.getOrThrow();
                    try {
                        var privateKey = SecurityHelper.toPrivateECDSAKey(Base64.getDecoder().decode(Launcher.getConfig().ecdsaBuildPrivateKey));
                        var publicKey = SecurityHelper.toPublicECDSAKey(Base64.getDecoder().decode(Launcher.getConfig().ecdsaBuildPublicKey));
                        var signedData = SecurityHelper.sign(Base64.getDecoder().decode(result.result().data()), privateKey);
                        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                                .POST(HttpHelper.jsonBodyPublisher(new HttpUpdatesCheck(
                                        Base64.getEncoder().encodeToString(signedData),
                                        Base64.getEncoder().encodeToString(publicKey.getEncoded()),
                                        result.result().jwtToken())))
                                .uri(URI.create(baseUrl.concat("/updates/check")))
                                        .header("Content-Type", "application/json")
                                .build(), new HttpErrorHandler<>(LauncherUpdateInfo.class))
                                .thenApply(HttpHelper.HttpOptional::getOrThrow);
                    } catch (InvalidKeySpecException e) {
                        return CompletableFuture.failedFuture(e);
                    }
        });
    }

    @Override
    public CompletableFuture<SecurityLevelInfo> getSecurityInfo() {
        // TODO: Implement
        return CompletableFuture.completedFuture(new SecurityLevelInfo() {
            @Override
            public boolean isRequired() {
                return false;
            }

            @Override
            public byte[] getSignData() {
                return new byte[0];
            }
        });
    }

    @Override
    public CompletableFuture<SecurityLevelVerification> privateKeyVerification(PublicKey publicKey, byte[] signature) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public CompletableFuture<Void> sendHardwareInfo(HardwareStatisticData statisticData, HardwareIdentifyData identifyData) {
        // TODO: Implement
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    public record HttoAuthMethod() implements AuthMethod {

        @Override
        public List<AuthMethodDetails> getDetails() {
            return List.of(new AuthPasswordDetails(), new AuthTotpDetails(6));
        }

        @Override
        public String getName() {
            return "Http";
        }

        @Override
        public String getDisplayName() {
            return "Http";
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public Set<String> getFeatures() {
            return Set.of();
        }
    }

    public static class HttpUser implements User {

        public String username;
        public UUID uuid;
        public Map<String, pro.gravit.launcher.base.profiles.Texture> assets;
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
            return (Map<String, Texture>) (Map) assets;
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

    public static class HttpErrorHandler<T> implements HttpHelper.HttpJsonErrorHandler<T, ErrorResponse> {
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

    public record HttpUpdatesPrepare(String data, String jwtToken) {}

    public record HttpUpdatesCheck(String signedData, String publicKey, String jwtToken) {}

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
