package pro.gravit.launcher.base.request;

import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.HttpHelper;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launcher.core.api.features.UserFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodDetails;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.method.details.AuthDeviceFlowDetails;
import pro.gravit.launcher.core.api.method.details.AuthWebDetails;
import pro.gravit.launcher.core.api.method.password.AuthDeviceCodePassword;
import pro.gravit.launcher.core.api.method.password.AuthOAuthPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.User;
import pro.gravit.launcher.core.api.model.UserPermissions;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class RequestMicrosoftAPIImpl implements CoreFeatureAPI, AuthFeatureAPI, UserFeatureAPI {
    private static final Pattern UUID_REGEX = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    private static final String MICROSOFT_SCOPE = "XboxLive.signin offline_access";
    private static final Logger logger = LoggerFactory.getLogger(RequestMicrosoftAPIImpl.class);
    private final HttpClient client = HttpClient.newBuilder().build();
    private final String clientId;
    private final String clientSecret;
    private final AtomicReference<DeviceCodeResponse> deviceCodeRef = new AtomicReference<>();
    private final AtomicReference<MicrosoftAuthData> authDataRef = new AtomicReference<>();
    private final AtomicReference<MicrosoftUser> userRef = new AtomicReference<>();

    public RequestMicrosoftAPIImpl() {
        this("d772766b-19b4-4f69-b353-989f890c5d3b", null);
    }

    public RequestMicrosoftAPIImpl(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public static UUID getUUIDFromMojangHash(String hash) {
        return UUID.fromString(UUID_REGEX.matcher(hash).replaceFirst("$1-$2-$3-$4-$5"));
    }

    @Override
    public CompletableFuture<List<AuthMethod>> getAuthMethods() {
        return CompletableFuture.completedFuture(List.of(new MicrosoftAuthMethod(new AuthDeviceFlowDetails("https://www.microsoft.com/link", () -> {
            return sendMicrosoftDeviceCodeRequest(MICROSOFT_SCOPE).thenApply(device -> {
                deviceCodeRef.set(device);
                return new AuthDeviceFlowDetails.AuthDeviceFlowDetailsData(device.device_code, device.user_code);
            });
        }, true))));

    }

    @Override
    public CompletableFuture<LauncherUpdateInfo> checkUpdates() {
        return CompletableFuture.completedFuture(new LauncherUpdateInfo(null, "Unknown", false, false));
    }

    @Override
    public CompletableFuture<SelfUser> getCurrentUser() {
        MicrosoftAuthData authData = authDataRef.get();
        if (authData == null) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        MicrosoftUser user = userRef.get();
        if (user != null) {
            return CompletableFuture.completedFuture(user);
        }
        return getUserSessionByOAuthAccessToken(authData.accessToken()).thenApply(result -> {
            userRef.set(result);
            return result;
        });
    }

    @Override
    public CompletableFuture<AuthResponse> auth(String login, AuthMethodPassword password) {
        if (!(password instanceof AuthDeviceCodePassword(String deviceCode))) {
            return CompletableFuture.failedFuture(new RequestException("Microsoft auth requires OAuth password"));
        }
        logger.debug("Microsoft auth found device code {}", deviceCode);
        return tryGetDeviceToken(deviceCode)
                .thenCompose(token -> getMinecraftTokenByMicrosoftToken(token.access_token())
                        .thenCompose(minecraftToken -> getUserSessionByOAuthAccessToken(minecraftToken.access_token())
                                .thenApply(user -> {
                                    MicrosoftAuthData authData = new MicrosoftAuthData(minecraftToken.access_token(), token.refresh_token(), minecraftToken.expires_in());
                                    authDataRef.set(authData);
                                    userRef.set(user);
                                    return new AuthResponse(user, authData);
                                })));
    }

    @Override
    public CompletableFuture<AuthToken> refreshToken(String refreshToken) {
        return sendMicrosoftOAuthRefreshTokenRequest(refreshToken)
                .thenCompose(token -> getMinecraftTokenByMicrosoftToken(token.access_token())
                        .thenApply(minecraftToken -> {
                            MicrosoftAuthData authData = new MicrosoftAuthData(minecraftToken.access_token(), token.refresh_token(), minecraftToken.expires_in());
                            authDataRef.set(authData);
                            userRef.set(null);
                            return authData;
                        }));
    }

    @Override
    public CompletableFuture<SelfUser> restore(String accessToken, boolean fetchUser) {
        MicrosoftAuthData authData = new MicrosoftAuthData(accessToken, null, 0);
        authDataRef.set(authData);
        userRef.set(null);
        if (!fetchUser) {
            return CompletableFuture.completedFuture(null);
        }
        return getCurrentUser();
    }

    @Override
    public CompletableFuture<Void> exit() {
        authDataRef.set(null);
        userRef.set(null);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<User> getUserByUsername(String username) {
        return sendMojangRequest("GET", "https://api.mojang.com/users/profiles/minecraft/%s"
                .formatted(URLEncoder.encode(username, StandardCharsets.UTF_8)), null, null, MojangUUIDResponse.class)
                .thenCompose(response -> response == null ? CompletableFuture.completedFuture(null) : getUserByHash(response.id()).thenApply(User.class::cast));
    }

    @Override
    public CompletableFuture<User> getUserByUUID(UUID uuid) {
        return getUserByHash(uuid.toString().replaceAll("-", "")).thenApply(User.class::cast);
    }

    @Override
    public CompletableFuture<Void> joinServer(String username, String accessToken, String serverID) {
        MicrosoftUser user = userRef.get();
        if (user == null) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return joinServer(user.getUUID(), accessToken, serverID);
    }

    @Override
    public CompletableFuture<Void> joinServer(UUID uuid, String accessToken, String serverID) {
        MicrosoftAuthData authData = authDataRef.get();
        if (authData == null) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return sendMojangRequest("POST", "https://sessionserver.mojang.com/session/minecraft/join", null,
                new MojangJoinServerRequest(accessToken == null ? authData.accessToken() : accessToken, uuid, serverID), Void.class)
                .thenApply(result -> null);
    }

    @Override
    public CompletableFuture<CheckServerResponse> checkServer(String username, String serverID, boolean extended) {
        return sendMojangRequest("GET", "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s"
                .formatted(URLEncoder.encode(username, StandardCharsets.UTF_8), URLEncoder.encode(serverID, StandardCharsets.UTF_8)), null, null, MojangProfileResponse.class)
                .thenApply(response -> response == null ? null : new CheckServerResponse(getUserByProfileResponse(response), null, null, Map.of()));
    }

    private String getDeviceCode(String redirectUrl) {
        if (redirectUrl == null || !redirectUrl.startsWith("device:")) {
            DeviceCodeResponse device = deviceCodeRef.get();
            if (device == null) {
                throw new RequestException("Microsoft device code is not initialized");
            }
            return device.device_code();
        }
        String marker = redirectUrl.substring("device:".length());
        int separator = marker.indexOf(':');
        return separator < 0 ? marker : marker.substring(0, separator);
    }

    private CompletableFuture<MicrosoftUser> getUserSessionByOAuthAccessToken(String accessToken) {
        return sendMojangRequest("GET", "https://api.minecraftservices.com/minecraft/profile", accessToken, null, MojangProfileByTokenResponse.class)
                .thenApply(response -> {
                    if (response == null) {
                        throw new RequestException("OAuth access token expired");
                    }
                    return getUserByTokenResponse(response, accessToken);
                });
    }

    private CompletableFuture<MicrosoftUser> getUserByHash(String hash) {
        return sendMojangRequest("GET", "https://sessionserver.mojang.com/session/minecraft/profile/%s".formatted(hash), null, null, MojangProfileResponse.class)
                .thenApply(response -> response == null ? null : getUserByProfileResponse(response));
    }

    private MicrosoftUser getUserByProfileResponse(MojangProfileResponse response) {
        Map<String, Texture> assets = new HashMap<>();
        MojangProfilePropertyTexture textures = response.getTextures();
        if (textures != null && textures.textures() != null) {
            for (var entry : textures.textures().entrySet()) {
                assets.put(entry.getKey(), entry.getValue().toTexture());
            }
        }
        return new MicrosoftUser(response.name(), getUUIDFromMojangHash(response.id()), assets, Map.of(), null);
    }

    private MicrosoftUser getUserByTokenResponse(MojangProfileByTokenResponse response, String accessToken) {
        Map<String, Texture> assets = new HashMap<>();
        response.skins().stream()
                .filter(MojangProfileByTokenTextureResponse::isActive)
                .findFirst()
                .map(MojangProfileByTokenTextureResponse::toTexture)
                .ifPresent(texture -> assets.put("SKIN", texture));
        response.capes().stream()
                .filter(MojangProfileByTokenTextureResponse::isActive)
                .findFirst()
                .map(MojangProfileByTokenTextureResponse::toTexture)
                .ifPresent(texture -> assets.put("CAPE", texture));
        return new MicrosoftUser(response.name(), getUUIDFromMojangHash(response.id()), assets, Map.of(), accessToken);
    }

    private CompletableFuture<MinecraftLoginWithXBoxResponse> getMinecraftTokenByMicrosoftToken(String microsoftAccessToken) {
        return sendMicrosoftXBoxLiveRequest(microsoftAccessToken)
                .thenCompose(xboxLive -> sendMicrosoftXSTSRequest(xboxLive.Token()))
                .thenCompose(xsts -> checkMinecraftOwnership(xsts.Token())
                        .thenCompose(result -> sendMinecraftLoginWithXBoxRequest(xsts.getUHS(), xsts.Token())));
    }

    private CompletableFuture<DeviceCodeResponse> sendMicrosoftDeviceCodeRequest(String scope) {
        String body = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        return HttpHelper.sendAsync(client, request, new MicrosoftErrorHandler<>(DeviceCodeResponse.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    private CompletableFuture<Void> checkMinecraftOwnership(String xstsToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/entitlements/mcstore"))
                .header("Authorization", "Bearer " + xstsToken)
                .GET()
                .build();
        return HttpHelper.sendAsync(client, request, new HttpHelper.BasicJsonHttpErrorHandler<>(JsonElement.class)).thenApply(result -> null);
    }

    private CompletableFuture<MicrosoftOAuthTokenResponse> tryGetDeviceToken(String deviceCode) {
        String body = "grant_type=urn:ietf:params:oauth:grant-type:device_code"
                + "&device_code=" + URLEncoder.encode(deviceCode, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        return HttpHelper.sendAsync(client, request, new MicrosoftErrorHandler<>(MicrosoftOAuthTokenResponse.class))
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        return result.getOrThrow();
                    }
                    Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;
                    String message = cause.toString();
                    if (message.contains("authorization_pending")) {
                        throw new RequestException("Microsoft authorization not completed yet. Please finish sign-in in browser and try again.");
                    }
                    if (message.contains("expired_token")) {
                        throw new RequestException("Device code expired. Please restart the login process.");
                    }
                    throw new CompletionException(cause);
                });
    }

    private CompletableFuture<MicrosoftOAuthTokenResponse> sendMicrosoftOAuthRefreshTokenRequest(String refreshToken) {
        String body = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + (clientSecret == null ? "" : "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8))
                + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                + "&grant_type=refresh_token";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        return HttpHelper.sendAsync(client, request, new MicrosoftErrorHandler<>(MicrosoftOAuthTokenResponse.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    private CompletableFuture<MicrosoftXBoxLiveResponse> sendMicrosoftXBoxLiveRequest(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpHelper.jsonBodyPublisher(new MicrosoftXBoxLiveRequest(accessToken)))
                .build();
        return HttpHelper.sendAsync(client, request, new HttpHelper.BasicJsonHttpErrorHandler<>(MicrosoftXBoxLiveResponse.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    private CompletableFuture<MicrosoftXBoxLiveResponse> sendMicrosoftXSTSRequest(String xblToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpHelper.jsonBodyPublisher(new MicrosoftXSTSRequest(xblToken)))
                .build();
        return HttpHelper.sendAsync(client, request, new XSTSErrorHandler<>(MicrosoftXBoxLiveResponse.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    private CompletableFuture<MinecraftLoginWithXBoxResponse> sendMinecraftLoginWithXBoxRequest(String uhs, String xstsToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpHelper.jsonBodyPublisher(new MinecraftLoginWithXBoxRequest(uhs, xstsToken)))
                .build();
        return HttpHelper.sendAsync(client, request, new HttpHelper.BasicJsonHttpErrorHandler<>(MinecraftLoginWithXBoxResponse.class)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    private <T, V> CompletableFuture<T> sendMojangRequest(String method, String url, String accessToken, V body, Class<T> clazz) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpHelper.jsonBodyPublisher(body))
                .uri(URI.create(url))
                .header("Accept", "application/json");
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (accessToken != null) {
            builder.header("Authorization", "Bearer ".concat(accessToken));
        }
        return HttpHelper.sendAsync(client, builder.build(), new MojangErrorHandler<>(clazz)).thenApply(HttpHelper.HttpOptional::getOrThrow);
    }

    public record MicrosoftAuthMethod(AuthDeviceFlowDetails details) implements AuthMethod {
        @Override
        public List<AuthMethodDetails> getDetails() {
            return List.of(details);
        }

        @Override
        public String getName() {
            return "microsoft";
        }

        @Override
        public String getDisplayName() {
            return "Microsoft";
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public Set<String> getFeatures() {
            return Set.of("externalBrowser");
        }
    }

    public record MicrosoftAuthData(String accessToken, String refreshToken, long expire) implements AuthToken {
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
            return expire;
        }
    }

    public record MicrosoftUser(String username, UUID uuid, Map<String, Texture> assets, Map<String, String> properties,
                                String accessToken) implements SelfUser {
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

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public UserPermissions getPermissions() {
            return new ClientPermissions();
        }
    }

    public record MicrosoftTexture(String url, String hash, Map<String, String> metadata) implements Texture {
        public MicrosoftTexture(String url, byte[] hash, Map<String, String> metadata) {
            this(url, SecurityHelper.toHex(hash), metadata);
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public String getHash() {
            return hash;
        }

        @Override
        public Map<String, String> getMetadata() {
            return metadata;
        }
    }

    public record MojangUUIDResponse(String name, String id) {
    }

    public record MojangProfileResponse(String id, String name, List<MojangProfileProperty> properties) {
        public MojangProfilePropertyTexture getTextures() {
            if (properties == null) {
                return null;
            }
            for (var property : properties) {
                if ("textures".equals(property.name())) {
                    String jsonData = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
                    return Launcher.gsonManager.gson.fromJson(jsonData, MojangProfilePropertyTexture.class);
                }
            }
            return null;
        }
    }

    public record MojangProfileProperty(String name, String value, String signature) {
    }

    public record MojangProfilePropertyTexture(String profileId, String profileName, boolean signatureRequired,
                                               Map<String, MojangProfileTexture> textures) {
    }

    public record MojangProfileTexture(String url, String digest, Map<String, String> metadata) {
        public Texture toTexture() {
            return new MicrosoftTexture(url, digest == null ? SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, url) : SecurityHelper.fromHex(digest), metadata);
        }
    }

    public record MojangProfileByTokenResponse(String id, String name, List<MojangProfileByTokenTextureResponse> skins,
                                               List<MojangProfileByTokenTextureResponse> capes) {
        public MojangProfileByTokenResponse {
            skins = skins == null ? List.of() : skins;
            capes = capes == null ? List.of() : capes;
        }
    }

    public record MojangProfileByTokenTextureResponse(String id, String state, String url, String digest,
                                                      String variant) {
        public boolean isActive() {
            return "ACTIVE".equals(state);
        }

        public Texture toTexture() {
            Map<String, String> metadata = "SLIM".equals(variant) ? Map.of("model", "slim") : Map.of();
            return new MicrosoftTexture(url, digest == null ? SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, url) : SecurityHelper.fromHex(digest), metadata);
        }
    }

    public record MojangJoinServerRequest(String accessToken, String selectedProfile, String serverId) {
        public MojangJoinServerRequest(String accessToken, UUID uuid, String serverId) {
            this(accessToken, uuid.toString().replaceAll("-", ""), serverId);
        }
    }

    public record MicrosoftOAuthTokenResponse(String token_type, long expires_in, String scope, String access_token,
                                              String refresh_token, String user_id, String foci) {
    }

    public record DeviceCodeResponse(String device_code, String user_code, String verification_uri,
                                     String verification_uri_complete, long expires_in, long interval) {
    }

    public record MicrosoftXBoxLivePropertiesRequest(String AuthMethod, String SiteName, String RpsTicket) {
        public MicrosoftXBoxLivePropertiesRequest(String accessToken) {
            this("RPS", "user.auth.xboxlive.com", "d=".concat(accessToken));
        }
    }

    public record MicrosoftXBoxLiveRequest(MicrosoftXBoxLivePropertiesRequest Properties, String RelyingParty,
                                           String TokenType) {
        public MicrosoftXBoxLiveRequest(String accessToken) {
            this(new MicrosoftXBoxLivePropertiesRequest(accessToken), "http://auth.xboxlive.com", "JWT");
        }
    }

    public record MicrosoftXBoxLiveResponse(String IssueInstant, String NotAfter, String Token,
                                            Map<String, List<Map<String, String>>> DisplayClaims) {
        public String getUHS() {
            return DisplayClaims.get("xui").getFirst().get("uhs");
        }
    }

    public record MicrosoftXSTSPropertiesRequest(String SandboxId, List<String> UserTokens) {
        public MicrosoftXSTSPropertiesRequest(String xblToken) {
            this("RETAIL", List.of(xblToken));
        }
    }

    public record MicrosoftXSTSRequest(MicrosoftXSTSPropertiesRequest Properties, String RelyingParty,
                                       String TokenType) {
        public MicrosoftXSTSRequest(String xblToken) {
            this(new MicrosoftXSTSPropertiesRequest(xblToken), "rp://api.minecraftservices.com/", "JWT");
        }
    }

    public record MinecraftLoginWithXBoxRequest(String identityToken) {
        public MinecraftLoginWithXBoxRequest(String uhs, String xstsToken) {
            this("XBL3.0 x=%s;%s".formatted(uhs, xstsToken));
        }
    }

    public record MinecraftLoginWithXBoxResponse(String username, List<String> roles, String access_token,
                                                 String token_type, long expires_in) {
    }

    public record MicrosoftError(String error, String error_description, String correlation_id) {
        @Override
        public String toString() {
            return error_description == null ? error : error_description;
        }
    }

    public record XSTSError(String Identity, long XErr, String Message, String Redirect) {
        @Override
        public String toString() {
            if (Message != null && !Message.isEmpty()) {
                return Message;
            }
            if (XErr == 2148916233L) {
                return "The account doesn't have an Xbox account.";
            }
            if (XErr == 2148916235L) {
                return "The account is from a country where Xbox Live is not available/banned";
            }
            if (XErr == 2148916238L) {
                return "The account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult";
            }
            return "XSTS error: %d".formatted(XErr);
        }
    }

    public record MojangError(String error, String errorMessage) {
        @Override
        public String toString() {
            return errorMessage == null ? error : errorMessage;
        }
    }

    private static class MicrosoftErrorHandler<T> implements HttpHelper.HttpJsonErrorHandler<T, MicrosoftError> {
        private final Class<T> type;

        private MicrosoftErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, MicrosoftError> applyJson(JsonElement response, int statusCode) {
            if (statusCode < 200 || statusCode >= 300) {
                return new HttpHelper.HttpOptional<>(null, Launcher.gsonManager.gson.fromJson(response, MicrosoftError.class), statusCode);
            }
            return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(response, type), null, statusCode);
        }
    }

    private static class XSTSErrorHandler<T> implements HttpHelper.HttpJsonErrorHandler<T, XSTSError> {
        private final Class<T> type;

        private XSTSErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, XSTSError> applyJson(JsonElement response, int statusCode) {
            if (statusCode < 200 || statusCode >= 300) {
                return new HttpHelper.HttpOptional<>(null, Launcher.gsonManager.gson.fromJson(response, XSTSError.class), statusCode);
            }
            return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(response, type), null, statusCode);
        }
    }

    private static class MojangErrorHandler<T> implements HttpHelper.HttpErrorHandler<T, MojangError> {
        private final Class<T> type;

        private MojangErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, MojangError> apply(HttpResponse<InputStream> response) {
            int statusCode = response.statusCode();
            if (statusCode == 204) {
                return new HttpHelper.HttpOptional<>(null, null, statusCode);
            }
            JsonElement element = null;
            try (Reader reader = new InputStreamReader(response.body(), StandardCharsets.UTF_8)) {
                element = Launcher.gsonManager.gson.fromJson(reader, JsonElement.class);
            } catch (Exception e) {
                if (statusCode >= 200 && statusCode < 300) {
                    return new HttpHelper.HttpOptional<>(null, null, statusCode);
                }
            }
            if (statusCode < 200 || statusCode >= 300) {
                MojangError error = element == null || element.isJsonNull()
                        ? new MojangError("HTTP_" + statusCode, null)
                        : Launcher.gsonManager.gson.fromJson(element, MojangError.class);
                return new HttpHelper.HttpOptional<>(null, error, statusCode);
            }
            return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(element, type), null, statusCode);
        }
    }
}
