package pro.gravit.launcher.base.request;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.HttpHelper;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.request.update.LauncherRequest;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
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
import java.security.SignatureException;
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

    private CompletableFuture<SelfUser> getMinecraftProfile(String token, String accountName) {
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder().GET()
                .uri(URI.create(baseUrl.concat("/minecraft/profile")))
                .header("Authorization", "Bearer "+token).build(), new HttpErrorHandler<>(HttpMinecraftProfile.class))
                .thenApply(r -> {
                    var p = r.getOrThrow();
                    var user = new HttpSelfUser();
                    user.username = p.name == null ? accountName : p.name;
                    user.uuid = UUID.fromString(p.id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                    user.assets = new HashMap<>();
                    for (var skin : p.skins) user.assets.put("SKIN", new pro.gravit.launcher.base.profiles.Texture(skin.url, new byte[0], Map.of("variant", skin.variant)));
                    return (SelfUser) user;
                });
    }

    private static HttpUser toUser(HttpLookupProfile profile) {
        Map<String, pro.gravit.launcher.base.profiles.Texture> assets = new HashMap<>();
        Map<String, String> properties = new HashMap<>();
        for (HttpMinecraftSessionProperty property : profile.properties) {
            properties.put(property.name(), property.value());
            if (!"textures".equals(property.name())) continue;
            try {
                JsonElement textureData = Launcher.gsonManager.gson.fromJson(
                        new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8), JsonElement.class);
                var textures = textureData.getAsJsonObject().getAsJsonObject("textures");
                for (var entry : textures.entrySet()) {
                    var texture = entry.getValue().getAsJsonObject();
                    Map<String, String> metadata = new HashMap<>();
                    if (texture.has("metadata")) {
                        for (var meta : texture.getAsJsonObject("metadata").entrySet()) metadata.put(meta.getKey(), meta.getValue().getAsString());
                    }
                    String textureUrl = texture.get("url").getAsString();
                    byte[] textureHash = texture.has("hash") && !texture.get("hash").isJsonNull()
                            ? SecurityHelper.fromHex(texture.get("hash").getAsString()) : null;
                    assets.put(entry.getKey(), new pro.gravit.launcher.base.profiles.Texture(
                            textureUrl, textureHash, metadata));
                }
            } catch (RuntimeException ignored) {
                // Preserve the raw property when texture payload decoding is unavailable.
            }
        }
        return new HttpUser(profile.name, UUID.fromString(profile.id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), assets, properties);
    }

    @Override
    public CompletableFuture<SelfUser> getCurrentUser() {
        var accessToken = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
        if(accessToken.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized (currentuser)"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl.concat("/users/me")))
                .header("Authorization", "Bearer "+accessToken.get())
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpAccountUser.class)).thenCompose(result -> {
            var res = result.result();
            if(res == null) {
                return CompletableFuture.completedFuture(null);
            }
            return getMinecraftProfile(accessToken.get(), res.username);
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
                .POST(HttpHelper.jsonBodyPublisher(new HttpRefreshRequest(Optional.ofNullable(authDataRef.get()).map(e -> e.refreshToken).orElse(""))))
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
                .uri(URI.create(baseUrl.concat("/minecraft/profile/lookup/name/").concat(URLEncoder.encode(username, StandardCharsets.UTF_8))))
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpLookupProfile.class)).thenApply(e -> e.result() == null ? null : toUser(e.result()));
    }

    @Override
    public CompletableFuture<User> getUserByUUID(UUID uuid) {
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl.concat("/minecraft/profile/lookup/").concat(uuid.toString().replace("-", ""))))
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpLookupProfile.class)).thenApply(e -> e.result() == null ? null : toUser(e.result()));
    }

    @Override
    public CompletableFuture<Void> joinServer(String username, String accessToken, String serverID) {
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                        .POST(HttpHelper.jsonBodyPublisher(new HttpMinecraftJoinRequest(accessToken == null ? accessToken0.get() : accessToken, username, serverID)))
                        .uri(URI.create(baseUrl.concat("/minecraft/sessionserver/session/minecraft/join")))
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
                        .POST(HttpHelper.jsonBodyPublisher(new HttpMinecraftJoinRequest(accessToken == null ? accessToken0.get() : accessToken, uuid.toString().replace("-", ""), serverID)))
                        .uri(URI.create(baseUrl.concat("/minecraft/sessionserver/session/minecraft/join")))
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
                        .GET()
                        .uri(URI.create(baseUrl.concat("/minecraft/sessionserver/session/minecraft/hasJoined?username=") + URLEncoder.encode(username, StandardCharsets.UTF_8) + "&serverId=" + URLEncoder.encode(serverID, StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer "+accessToken.get())
                        .header("Content-Type", "application/json")
                        .build(), new HttpErrorHandler<>(HttpMinecraftSessionProfile.class))
                .thenApply(e -> e.result() == null ? null : new CheckServerResponse(toUser(new HttpLookupProfile(e.result().id, e.result().name)), null, null, Map.of()));
    }

    @Override
    public CompletableFuture<List<ClientProfile>> getProfiles() {
        var accessToken0 = Optional.ofNullable(authDataRef.get()).map(e -> e.token);
        if(accessToken0.isEmpty()) {
            return CompletableFuture.failedFuture(new RequestException("You are not authorized"));
        }
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(baseUrl.concat("/cas/directories/list")))
                        .header("Authorization", "Bearer "+accessToken0.get())
                        .header("Content-Type", "application/json")
                        .build(), new HttpErrorHandler<>(JsonArray.class))
                .thenCompose(e -> {
                    Map<UUID, pro.gravit.launcher.base.profiles.ClientProfile> profiles = new LinkedHashMap<>();
                    for (JsonElement element : e.getOrThrow()) {
                        HttpCasDirectory directory = Launcher.gsonManager.gson.fromJson(element, HttpCasDirectory.class);
                        HttpCasProfileMetadata metadata = Launcher.gsonManager.gson.fromJson(directory.metadata(), HttpCasProfileMetadata.class);
                        pro.gravit.launcher.base.profiles.ClientProfile profile = new ClientProfileBuilder()
                                .setTitle(metadata.name())
                                .setInfo("")
                                .setDir(directory.key())
                                .setUuid(metadata.uuid())
                                .createClientProfile();
                        profiles.putIfAbsent(profile.getUUID(), profile);
                    }
                    List<CompletableFuture<pro.gravit.launcher.base.profiles.ClientProfile>> requests = new ArrayList<>();
                    for (pro.gravit.launcher.base.profiles.ClientProfile fallback : profiles.values()) {
                        requests.add(loadPublishedProfile(fallback, accessToken0.get()));
                    }
                    return CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new))
                            .thenApply(v -> requests.stream().map(CompletableFuture::join)
                                    .map(profile -> (ClientProfile) profile).toList());
                });
    }

    private CompletableFuture<pro.gravit.launcher.base.profiles.ClientProfile> loadPublishedProfile(pro.gravit.launcher.base.profiles.ClientProfile fallback, String token) {
        String uri = baseUrl + "/cas/versions/latest?directoryKey="
                + URLEncoder.encode(fallback.getDir(), StandardCharsets.UTF_8) + "&branchName=main";
        return HttpHelper.sendAsync(client, HttpRequest.newBuilder().GET().uri(URI.create(uri))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json").build(), new HttpErrorHandler<>(HttpCasVersion.class))
                .thenApply(response -> {
                    if (response.result() == null || response.result().metadata() == null) return fallback;
                    HttpCasVersionMetadata metadata = Launcher.gsonManager.gson.fromJson(response.result().metadata(), HttpCasVersionMetadata.class);
                    if (metadata.profile() == null) return normalizeProfile(fallback);
                    return normalizeProfile(new ClientProfileBuilder(metadata.profile())
                            .setUuid(fallback.getUUID())
                            .setDir(fallback.getDir())
                            .createClientProfile());
                })
                .exceptionally(error -> normalizeProfile(fallback));
    }

    private pro.gravit.launcher.base.profiles.ClientProfile normalizeProfile(pro.gravit.launcher.base.profiles.ClientProfile profile) {
        if (profile.getVersion() != null) return profile;
        return new ClientProfileBuilder(profile)
                .setVersion(pro.gravit.launcher.base.profiles.ClientProfile.Version.of("0"))
                .createClientProfile();
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
                        .uri(URI.create(baseUrl.concat(String.format("/cas/versions/latest?directoryKey=%s&branchName=main",
                                URLEncoder.encode(((pro.gravit.launcher.base.profiles.ClientProfile) profileRef.get()).getDir(), StandardCharsets.UTF_8)))))
                        .header("Authorization", "Bearer "+accessToken0.get())
                .header("Content-Type", "application/json")
                .build(), new HttpErrorHandler<>(HttpCasVersion.class))
                .thenApply(HttpHelper.HttpOptional::getOrThrow)
                .thenApply(version -> {
                    String manifestRoot = dirName.toLowerCase(Locale.ROOT).contains("asset") ? "assets/" : "client/";
                    HashedDir dir = toHashedDir(version.manifest(), manifestRoot);
                    return new HttpUpdateInfo(dir, baseUrl);
                });
    }

    private HashedDir toHashedDir(String manifest, String rootPrefix) {
        HashedDir root = new HashedDir();
        JsonElement parsed = Launcher.gsonManager.gson.fromJson(manifest, JsonElement.class);
        JsonArray files = parsed.getAsJsonObject().getAsJsonArray("files");
        for (JsonElement element : files) {
            var file = element.getAsJsonObject();
            String path = file.get("path").getAsString();
            if (path.startsWith(rootPrefix)) {
                path = path.substring(rootPrefix.length());
            }
            if (path.isEmpty()) continue;
            long size = file.get("size").getAsLong();
            String storageKey = file.get("url").getAsString();
            var target = root.createParentDirectories(path);
            target.parent.put(target.name, new pro.gravit.launcher.core.hasher.HashedFile(size, null, storageKey));
        }
        return root;
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
                var signedData = SecurityHelper.sign(Base64.getDecoder().decode(res.challenge), privateKey);
                SecurityHelper.verifySign(Base64.getDecoder().decode(res.challenge), signedData, publicKey); // Self check
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
            } catch (InvalidKeySpecException | SignatureException e) {
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
        boolean cape = "CAPE".equalsIgnoreCase(name);
        String variant = settings != null && settings.slim() ? "slim" : "classic";
        byte[] preFileData;
        try(ByteArrayOutputStream output = new ByteArrayOutputStream(256)) {
            output.write("--".getBytes(StandardCharsets.UTF_8));
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
                .uri(URI.create(baseUrl.concat(cape
                        ? "/minecraft/profile/capes?alias=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                        : "/minecraft/profile/skins?variant=" + variant)))
                .POST(HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(preFileData),
                        HttpRequest.BodyPublishers.ofByteArray(bytes),
                        HttpRequest.BodyPublishers.ofByteArray(postFileData)))
                .header("Authorization", "Bearer "+accessToken0.get())
                .header("Content-Type", "multipart/form-data; boundary=\""+boundary+"\"")
                .header("Accept", "application/json")
                .build(), HttpResponse.BodyHandlers.ofByteArray()).thenCompose((response) -> {
            if(response.statusCode() >= 200 && response.statusCode() < 300) {
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(response.body()))) {
                    HttpMinecraftProfile profile = Launcher.gsonManager.gson.fromJson(reader, HttpMinecraftProfile.class);
                    if (cape) {
                        if (profile.capes == null || profile.capes.isEmpty()) {
                            return CompletableFuture.failedFuture(new RequestException("Server returned no uploaded cape"));
                        }
                        var texture = profile.capes.get(profile.capes.size() - 1);
                        return CompletableFuture.completedFuture(new pro.gravit.launcher.base.profiles.Texture(texture.url(), new byte[0], Map.of("alias", texture.alias())));
                    }
                    var texture = profile.skins.stream().filter(e -> variant.equalsIgnoreCase(e.variant())).findFirst()
                            .orElseThrow(() -> new RequestException("Server returned no uploaded skin"));
                    return CompletableFuture.completedFuture(new pro.gravit.launcher.base.profiles.Texture(texture.url(), new byte[0], Map.of("variant", texture.variant())));
                } catch (Throwable e) {
                    return CompletableFuture.failedFuture(e);
                }
            } else {
                try(Reader reader = new InputStreamReader(new ByteArrayInputStream(response.body()))) {
                    String body = new String(response.body(), StandardCharsets.UTF_8);
                    RequestFeatureAPIImpl.UploadError error = Launcher.gsonManager.gson.fromJson(body, RequestFeatureAPIImpl.UploadError.class);
                    String message = error == null || error.error() == null ? body : error.error();
                    return CompletableFuture.failedFuture(new RequestException("HTTP " + response.statusCode() + ": " + message));
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
            return Set.of(TextureUploadFeatureAPI.FEATURE_NAME);
        }
    }

    public static class HttpUser implements User {

        public String username;
        public UUID uuid;
        public Map<String, pro.gravit.launcher.base.profiles.Texture> assets;
        public Map<String, String> properties;

        public HttpUser() {}
        public HttpUser(String username, UUID uuid, Map<String, pro.gravit.launcher.base.profiles.Texture> assets, Map<String, String> properties) { this.username=username; this.uuid=uuid; this.assets=assets; this.properties=properties; }

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

    public static class HttpAccountUser {
        public String username;
    }
    public record HttpLookupProfile(String id, String name, List<HttpMinecraftSessionProperty> properties) {
        public HttpLookupProfile(String id, String name) {
            this(id, name, List.of());
        }

        public HttpLookupProfile {
            properties = properties == null ? List.of() : properties;
        }
    }
    public record HttpMinecraftJoinRequest(String accessToken, String selectedProfile, String serverId) {}
    public static class HttpMinecraftProfile {
        public String id, name;
        public List<HttpMinecraftSkin> skins = List.of();
        public List<HttpMinecraftCape> capes = List.of();
    }
    public record HttpMinecraftSkin(String id, String state, String url, String variant) {}
    public record HttpMinecraftCape(String id, String state, String url, String alias) {}
    public record HttpMinecraftSessionProfile(String id, String name, List<Object> properties) {}
    public record HttpMinecraftSessionProperty(String name, String value, String signature) {}

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

    public record HttpCasDirectory(long id, String key, String name, String metadata, String createdAt) {}

    public record HttpCasProfileMetadata(UUID uuid, String name) {}

    public record HttpCasVersionMetadata(pro.gravit.launcher.base.profiles.ClientProfile profile) {}

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

    public record HttpCasVersion(long id, long directoryId, String directoryKey, String branchName,
                                 Long parentVersionId, String manifestHash, String manifest, String metadata,
                                 String createdAt, Long createdBy, String message) {}

    public record HttpRefreshRequest(String refreshToken) {

    }

    record HttpCheckServerResponse(HttpUser user, String hardwareId, String sessionId, Map<String, String> sessionProperties) {
        CheckServerResponse toDefaultResult() {
            return new CheckServerResponse(user, hardwareId, sessionId, sessionProperties);
        }
    }
}
