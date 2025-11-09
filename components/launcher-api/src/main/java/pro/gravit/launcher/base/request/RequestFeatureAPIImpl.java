package pro.gravit.launcher.base.request;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.VerifySecureLevelKeyRequestEvent;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.request.auth.*;
import pro.gravit.launcher.base.request.auth.password.*;
import pro.gravit.launcher.base.request.cabinet.AssetUploadInfoRequest;
import pro.gravit.launcher.base.request.cabinet.GetAssetUploadUrl;
import pro.gravit.launcher.base.request.secure.GetSecureLevelInfoRequest;
import pro.gravit.launcher.base.request.secure.HardwareReportRequest;
import pro.gravit.launcher.base.request.secure.VerifySecureLevelKeyRequest;
import pro.gravit.launcher.base.request.update.ProfilesRequest;
import pro.gravit.launcher.base.request.update.UpdateRequest;
import pro.gravit.launcher.base.request.uuid.ProfileByUUIDRequest;
import pro.gravit.launcher.base.request.uuid.ProfileByUsernameRequest;
import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.core.api.features.*;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.method.password.AuthChainPassword;
import pro.gravit.launcher.core.api.method.password.AuthOAuthPassword;
import pro.gravit.launcher.core.api.method.password.AuthPlainPassword;
import pro.gravit.launcher.core.api.method.password.AuthTotpPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.User;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RequestFeatureAPIImpl implements AuthFeatureAPI, UserFeatureAPI, ProfileFeatureAPI, TextureUploadFeatureAPI, HardwareVerificationFeatureAPI {
    private final RequestService request;
    private final String authId;
    private final HttpClient client = HttpClient.newBuilder().build();

    public RequestFeatureAPIImpl(RequestService request, String authId) {
        this.request = request;
        this.authId = authId;
    }

    @Override
    public CompletableFuture<SelfUser> getCurrentUser() {
        return request.request(new CurrentUserRequest()).thenApply(response -> response.userInfo);
    }

    @Override
    public CompletableFuture<AuthResponse> auth(String login, AuthMethodPassword password) {
        AuthRequest.ConnectTypes connectType = AuthRequest.ConnectTypes.API;
        if(Request.getExtendedTokens() != null && Request.getExtendedTokens().get("launcher") != null) {
            connectType = AuthRequest.ConnectTypes.CLIENT;
        }
        return request.request(new AuthRequest(login, convertAuthPasswordAll(password), authId, false, connectType))
                .thenApply(response -> {
                    Request.setOAuth(authId, response.oauth);
                    return new AuthResponse(response.makeUserInfo(), response.oauth);
                });
    }

    private AuthRequest.AuthPasswordInterface convertAuthPasswordAll(AuthMethodPassword password) {
        AuthRequest.AuthPasswordInterface requestPassword;
        if(password instanceof AuthChainPassword(List<AuthMethodPassword> list)) {
            if(list.size() == 1) {
                requestPassword = convertAuthPassword(list.get(0));
            } else if(list.size() == 2) {
                requestPassword = new Auth2FAPassword(convertAuthPassword(list.get(0)),
                        convertAuthPassword(list.get(1)));
            } else {
                var multi = new AuthMultiPassword();
                for(var e : list) {
                    multi.list.add(convertAuthPassword(e));
                }
                requestPassword = multi;
            }
        } else {
            requestPassword = convertAuthPassword(password);
        }
        return requestPassword;
    }

    private AuthRequest.AuthPasswordInterface convertAuthPassword(AuthMethodPassword password) {
        if(password instanceof AuthPlainPassword(String value)) {
            String encryptKey = Launcher.getConfig().passwordEncryptKey;
            if(encryptKey != null) {
                try {
                    return new AuthAESPassword(SecurityHelper.encrypt(encryptKey, value));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return new pro.gravit.launcher.base.request.auth.password.AuthPlainPassword(value);
            }
        } else if(password instanceof AuthTotpPassword(String value)) {
            return new AuthTOTPPassword(value);
        } else if(password instanceof AuthOAuthPassword(String redirectUrl)) {
            return new AuthCodePassword(redirectUrl);
        } else if(password instanceof AuthRequest.AuthPasswordInterface custom) {
            return custom;
        } else if(password == null) {
            return null;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public CompletableFuture<User> getUserByUsername(String username) {
        return request.request(new ProfileByUsernameRequest(username)).thenApply(response -> response.playerProfile);
    }

    @Override
    public CompletableFuture<User> getUserByUUID(UUID uuid) {
        return request.request(new ProfileByUUIDRequest(uuid)).thenApply(response -> response.playerProfile);
    }

    @Override
    public CompletableFuture<Void> joinServer(String username, String accessToken, String serverID) {
        return request.request(new JoinServerRequest(username, accessToken, serverID)).thenCompose(response -> {
            if(response.allow) {
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.failedFuture(new RequestException("Not allowed"));
            }
        });
    }

    @Override
    public CompletableFuture<Void> joinServer(UUID uuid, String accessToken, String serverID) {
        return request.request(new JoinServerRequest(uuid, accessToken, serverID)).thenCompose(response -> {
            if(response.allow) {
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.failedFuture(new RequestException("Not allowed"));
            }
        });
    }

    @Override
    public CompletableFuture<CheckServerResponse> checkServer(String username, String serverID, boolean extended) {
        return request.request(new CheckServerRequest(username, serverID, extended, extended))
                .thenApply(response -> new CheckServerResponse(response.playerProfile, response.hardwareId,
                        response.sessionId, response.sessionProperties));
    }

    @Override
    public CompletableFuture<AuthToken> refreshToken(String refreshToken) {
        return request.request(new RefreshTokenRequest(authId, refreshToken)).thenApply(response -> response.oauth);
    }

    @Override
    public CompletableFuture<SelfUser> restore(String accessToken, boolean fetchUser) {
        Map<String, String> extended = new HashMap<>();
        if(Request.getExtendedTokens() != null) { // TODO: Control extended token
            for(var e : Request.getExtendedTokens().entrySet()) {
                extended.put(e.getKey(), e.getValue().token);
            }
        }
        return request.request(new RestoreRequest(authId, accessToken, extended, fetchUser)).thenApply(e -> {
            // TODO: invalidToken process
            Request.setOAuth(authId, new AuthRequestEvent.OAuthRequestEvent(accessToken, null, 0));
            return e.userInfo;
        });
    }

    @Override
    public CompletableFuture<Void> exit() {
        return request.request(new ExitRequest())
                .thenApply(response -> {
                    Request.setOAuth(null, null);
                    return null;
                });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public CompletableFuture<List<ProfileFeatureAPI.ClientProfile>> getProfiles() {
        return request.request(new ProfilesRequest()).thenApply(response -> (List) response.profiles);
    }

    @Override
    public CompletableFuture<Void> changeCurrentProfile(ClientProfile profile) {
        return request.request(new SetProfileRequest((pro.gravit.launcher.base.profiles.ClientProfile) profile)).thenApply(response -> null);
    }

    @Override
    public CompletableFuture<UpdateInfo> fetchUpdateInfo(String dirName) {
        return request.request(new UpdateRequest(dirName)).thenApply(response -> new UpdateInfoData(response.hdir, response.url));
    }

    @Override
    public CompletableFuture<TextureUploadInfo> fetchInfo() {
        return request.request(new AssetUploadInfoRequest()).thenApply(response -> response);
    }

    @Override
    public CompletableFuture<Texture> upload(String name, byte[] bytes, UploadSettings settings) {
        return request.request(new GetAssetUploadUrl(name)).thenCompose((response) -> {
            String accessToken = response.token == null ? Request.getAccessToken() : response.token.accessToken;
            String boundary = SecurityHelper.toHex(SecurityHelper.randomBytes(32));
            String jsonOptions = settings == null ? "{}" : Launcher.gsonManager.gson.toJson(new TextureUploadOptions(settings.slim()));
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
                    .uri(URI.create(response.url))
                    .POST(HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(preFileData),
                            HttpRequest.BodyPublishers.ofByteArray(bytes),
                            HttpRequest.BodyPublishers.ofByteArray(postFileData)))
                    .header("Authorization", "Bearer "+accessToken)
                    .header("Content-Type", "multipart/form-data; boundary=\""+boundary+"\"")
                    .header("Accept", "application/json")
                    .build(), HttpResponse.BodyHandlers.ofByteArray());
        }).thenCompose((response) -> {
            if(response.statusCode() >= 200 && response.statusCode() < 300) {
                try (Reader reader = new InputStreamReader(new ByteArrayInputStream(response.body()))) {
                    return CompletableFuture.completedFuture(Launcher.gsonManager.gson.fromJson(reader, UserTexture.class).toLauncherTexture());
                } catch (Throwable e) {
                    return CompletableFuture.failedFuture(e);
                }
            } else {
                try(Reader reader = new InputStreamReader(new ByteArrayInputStream(response.body()))) {
                    UploadError error = Launcher.gsonManager.gson.fromJson(reader, UploadError.class);
                    return CompletableFuture.failedFuture(new RequestException(error.error()));
                } catch (Exception ex) {
                    return CompletableFuture.failedFuture(ex);
                }
            }
        });
    }

    @Override
    public CompletableFuture<SecurityLevelInfo> getSecurityInfo() {
        return request.request(new GetSecureLevelInfoRequest()).thenApply(response -> response);
    }

    @Override
    public CompletableFuture<SecurityLevelVerification> privateKeyVerification(PublicKey publicKey, byte[] signature) {
        return request.request(new VerifySecureLevelKeyRequest(publicKey.getEncoded(), signature)).thenApply(response -> response);
    }

    @Override
    public CompletableFuture<Void> sendHardwareInfo(HardwareStatisticData statisticData, HardwareIdentifyData identifyData) {
        if(statisticData == null && identifyData == null) { // Hardware info token special
            return request.request(new HardwareReportRequest()).thenApply(response -> null);
        } else {
            var hardwareInfo = new HardwareReportRequest.HardwareInfo();
            if(statisticData != null) {
                hardwareInfo.bitness = statisticData.arch() == Arch.X86 || statisticData.arch() == Arch.ARM32 ? 32 : 64;
                hardwareInfo.totalMemory = statisticData.totalPhysicalMemory();
                hardwareInfo.logicalProcessors = statisticData.logicalProcessors();
                hardwareInfo.physicalProcessors = statisticData.physicalProcessors();
                hardwareInfo.processorMaxFreq = statisticData.processorMaxFreq();
                hardwareInfo.battery = statisticData.battery();
                hardwareInfo.graphicCard = statisticData.graphicCard();
            }
            if(identifyData != null) {
                hardwareInfo.hwDiskId = identifyData.persistentStorageId();
                hardwareInfo.displayId = identifyData.edid();
                hardwareInfo.baseboardSerialNumber = identifyData.baseboardSerialNumber();
            }
            return request.request(new HardwareReportRequest(hardwareInfo)).thenApply(response -> null);
        }
    }

    public record UpdateInfoData(HashedDir hdir, String url) implements ProfileFeatureAPI.UpdateInfo {
        @Override
        public HashedDir getHashedDir() {
            return hdir;
        }

        @Override
        public String getUrl() {
            return url;
        }
    }

    public record TextureUploadOptions(boolean modelSlim) {

    }

    public record UserTexture(@LauncherNetworkAPI String url, @LauncherNetworkAPI String digest, @LauncherNetworkAPI Map<String, String> metadata) {

        Texture toLauncherTexture() {
            return new pro.gravit.launcher.base.profiles.Texture(url, SecurityHelper.fromHex(digest), metadata);
        }
    }

    public record UploadError(@LauncherNetworkAPI String error) {

    }
}
