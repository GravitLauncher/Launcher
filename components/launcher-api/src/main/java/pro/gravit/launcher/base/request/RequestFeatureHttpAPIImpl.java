package pro.gravit.launcher.base.request;

import com.google.gson.JsonElement;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.HttpHelper;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.request.update.LauncherRequest;
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
import pro.gravit.utils.helper.SecurityHelper;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class RequestFeatureHttpAPIImpl implements AuthFeatureAPI, UserFeatureAPI, ProfileFeatureAPI, CoreFeatureAPI, HardwareVerificationFeatureAPI,TextureUploadFeatureAPI {
    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().build();
    private AtomicReference<HttpAuthData> authDataRef = new AtomicReference<>();
    private AtomicReference<ClientProfile> profileRef = new AtomicReference<>();
    private AtomicReference<String> launcherVerifyTokenRef = new AtomicReference<>();

    public RequestFeatureHttpAPIImpl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public CompletableFuture<SelfUser> getCurrentUser() {
        var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
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
            if(res == null) {
                return null;
            }
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
        var requestBuilder = HttpRequest.newBuilder()
                .POST(HttpHelper.jsonBodyPublisher(new HttpAuthRequest(login, rawPassword, rawTotp)))
                .uri(URI.create(baseUrl.concat("/auth/login")))
                .header("Content-Type", "application/json");
        String launcherVerifyToken = launcherVerifyTokenRef.get();
        if(launcherVerifyToken != null) {
            requestBuilder = requestBuilder
                    .header("X-Launcher-Update-Token", launcherVerifyToken);
        }
        return HttpHelper.sendAsync(client, requestBuilder
                .build(), new HttpErrorHandler<>(HttpAuthData.class)).thenCompose(result -> {
            authDataRef.set(result.getOrThrow());
            return getCurrentUser().thenApply((selfUser) -> new AuthResponse(selfUser, result.result()));
        });
    }

    @Override
    public CompletableFuture<AuthToken> refreshToken(String refreshToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .POST(HttpHelper.jsonBodyPublisher(new HttpRefreshRequest(refreshToken)))
                .uri(URI.create(baseUrl.concat("/auth/refresh")))
                .header("Content-Type", "application/json");

        String launcherVerifyToken = launcherVerifyTokenRef.get();
        if(launcherVerifyToken != null) {
            builder = builder
                    .header("X-Launcher-Update-Token", launcherVerifyToken);
        }
        return HttpHelper.sendAsync(client, builder
                .build(), new HttpErrorHandler<>(HttpAuthData.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    @Override
    public CompletableFuture<SelfUser> restore(String accessToken, boolean fetchUser) {
        authDataRef.set(new HttpAuthData(accessToken, null, 0));
        if(fetchUser) {
            return getCurrentUser();
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Void> exit() {
        var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
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
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                        .POST(HttpHelper.jsonBodyPublisher(new HttpJoinServerByUsernameRequest(username, serverID, accessToken0.get())))
                        .uri(URI.create(baseUrl.concat("/auth/joinserver/username")))
                        .header("Authorization", "Bearer "+accessToken0.get())
                        .header("Content-Type", "application/json")
                        .build(), new HttpErrorHandler<>(HttpCheckServerResponse.class))
                .thenApply(HttpHelper.HttpOptional::getOrThrow).thenApply(e -> null);
    }

    @Override
    public CompletableFuture<Void> joinServer(UUID uuid, String accessToken, String serverID) {
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                        .POST(HttpHelper.jsonBodyPublisher(new HttpJoinServerByUuidRequest(uuid.toString(), serverID, accessToken0.get())))
                        .uri(URI.create(baseUrl.concat("/auth/joinserver/uuid")))
                        .header("Authorization", "Bearer "+accessToken0.get())
                        .header("Content-Type", "application/json")
                        .build(), new HttpErrorHandler<>(HttpCheckServerResponse.class))
                .thenApply(HttpHelper.HttpOptional::getOrThrow).thenApply(e -> null);
    }

    @Override
    public CompletableFuture<CheckServerResponse> checkServer(String username, String serverID, boolean extended) {
        var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
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
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
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
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(baseUrl.concat(String.format("/profile/by/uuid/%s/dir/%s", profileRef.get().getUUID(), dirName))))
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
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(baseUrl.concat("/updates/prepare")))
                .build(), new HttpErrorHandler<>(HttpUpdatesPrepare.class)).thenCompose(result -> {
            var res = result.getOrThrow();
            try {
                var privateKey = SecurityHelper.toPrivateECDSAKey(Base64.getDecoder().decode(Launcher.getConfig().ecdsaBuildPrivateKey));
                var publicKey = SecurityHelper.toPublicECDSAKey(Base64.getDecoder().decode(Launcher.getConfig().ecdsaBuildPublicKey));
                var signedData = SecurityHelper.sign(Base64.getDecoder().decode(result.result().challenge), privateKey);
                return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                                .POST(HttpHelper.jsonBodyPublisher(new HttpUpdatesCheck(
                                        Base64.getEncoder().encodeToString(signedData),
                                        Base64.getEncoder().encodeToString(publicKey.getEncoded()),
                                        result.result().token())))
                                .uri(URI.create(baseUrl.concat("/updates/verify")))
                                .header("Content-Type", "application/json")
                                .build(), new HttpErrorHandler<>(HttpLauncherUpdateInfo.class))
                        .thenApply(HttpHelper.HttpOptional::getOrThrow)
                        .thenApply((httpLauncherUpdateInfo -> {
                            launcherVerifyTokenRef.set(httpLauncherUpdateInfo.token());
                            return new LauncherUpdateInfo(httpLauncherUpdateInfo.url, "1.0.0",
                                    httpLauncherUpdateInfo.updateRequired(), httpLauncherUpdateInfo.updateRequired());
                        }));
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

    @Override
    public CompletableFuture<TextureUploadInfo> fetchInfo() {
        return CompletableFuture.completedFuture(new HttpTextureUploadInfo());
    }

    @Override
    public CompletableFuture<Texture> upload(String name, byte[] bytes, UploadSettings settings) {
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        String boundary = SecurityHelper.toHex(SecurityHelper.randomBytes(32));
        String jsonOptions = settings == null ? "{}" : Launcher.gsonManager.gson.toJson(new RequestFeatureAPIImpl.TextureUploadOptions(settings.slim()));
        byte[] preFileData;
        try(ByteArrayOutputStream output = new ByteArrayOutputStream(256)) {
            output.write("--".getBytes(StandardCharsets.UTF_8));
            output.write(boundary.getBytes(StandardCharsets.UTF_8));
            output.write("\r\nContent-Disposition: form-data; name=\"options\"\r\nContent-Type: application/json\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(jsonOptions.getBytes(StandardCharsets.UTF_8));
            output.write("\r\n--".getBytes(StandardCharsets.UTF_8));
            output.write(boundary.getBytes(StandardCharsets.UTF_8));
            output.write("\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file\"\r\nContent-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            preFileData = output.toByteArray();
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(ex);
        }
        byte[] postFileData;
        try(ByteArrayOutputStream output = new ByteArrayOutputStream(128)) {
            output.write("\r\n--".getBytes(StandardCharsets.UTF_8));
            output.write(boundary.getBytes(StandardCharsets.UTF_8));
            output.write("--\r\n".getBytes(StandardCharsets.UTF_8));
            postFileData = output.toByteArray();
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(ex);
        }
        return client.sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl.concat("/cabinet/upload/"+name)))
                .POST(HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(preFileData),
                        HttpRequest.BodyPublishers.ofByteArray(bytes),
                        HttpRequest.BodyPublishers.ofByteArray(postFileData)))
                .header("Authorization", "Bearer "+accessToken0.get())
                .header("Content-Type", "multipart/form-data; boundary=\""+boundary+"\"")
                .header("Accept", "application/json")
                .build(), HttpResponse.BodyHandlers.ofByteArray()).thenCompose((response) -> {
            if(response.statusCode() >= 200 && response.statusCode() < 300) {
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(response.body()))) {
                    return CompletableFuture.completedFuture(Launcher.gsonManager.gson.fromJson(reader, RequestFeatureAPIImpl.UserTexture.class).toLauncherTexture());
                } catch (Throwable e) {
                    return CompletableFuture.failedFuture(e);
                }
            } else {
                try(Reader reader = new InputStreamReader(new ByteArrayInputStream(response.body()))) {
                    RequestFeatureAPIImpl.UploadError error = Launcher.gsonManager.gson.fromJson(reader, RequestFeatureAPIImpl.UploadError.class);
                    return CompletableFuture.failedFuture(new RequestException(error.error()));
                } catch (Exception ex) {
                    return CompletableFuture.failedFuture(ex);
                }
            }
        });
    }

    public record HttpTextureUploadInfo() implements TextureUploadInfo {

        @Override
        public Set<String> getAvailable() {
            return Set.of("SKIN", "CAPE");
        }

        @Override
        public boolean isRequireManualSlimSkinSelect() {
            return true;
        }
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
            return new ClientPermissions(new ArrayList<>(), new ArrayList<>());
        }
    }

    public static class HttpAuthData implements AuthToken {
        public String token;
        public String refreshToken;
        public long expireSeconds;

        public HttpAuthData() {
        }

        public HttpAuthData(String accessToken, String refreshToken, long expireSeconds) {
            this.token = accessToken;
            this.refreshToken = refreshToken;
            this.expireSeconds = expireSeconds;
        }

        @Override
        public String getAccessToken() {
            return token;
        }

        @Override
        public String getRefreshToken() {
            return refreshToken;
        }

        @Override
        public long getExpire() {
            return 0;
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

    public record HttpLauncherUpdateInfo(String url, Boolean updateRequired, String token) {
    }

    public record ErrorResponse(String code, String error) {
    }

    public record HttpAuthRequest(String username, String password, String totp) {

    }

    public record HttpCheckServerRequest(String username, String serverID, boolean extended) {

    }

    public record HttpJoinServerByUsernameRequest(String username, String serverID, String accessToken) {

    }

    public record HttpListProfilesResponse(List<pro.gravit.launcher.base.profiles.ClientProfile> profiles) {

    }

    public record HttpJoinServerByUuidRequest(String uuid, String serverID, String accessToken) {

    }

    public record HttpUpdatesPrepare(String challenge, String token) {}

    public record HttpUpdatesCheck(String signature, String publicKey, String token) {}

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
