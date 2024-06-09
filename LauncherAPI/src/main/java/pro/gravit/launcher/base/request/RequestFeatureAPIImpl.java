package pro.gravit.launcher.base.request;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.request.auth.*;
import pro.gravit.launcher.base.request.auth.password.*;
import pro.gravit.launcher.base.request.update.ProfilesRequest;
import pro.gravit.launcher.base.request.update.UpdateRequest;
import pro.gravit.launcher.base.request.uuid.ProfileByUUIDRequest;
import pro.gravit.launcher.base.request.uuid.ProfileByUsernameRequest;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.features.UserFeatureAPI;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.method.password.AuthChainPassword;
import pro.gravit.launcher.core.api.method.password.AuthOAuthPassword;
import pro.gravit.launcher.core.api.method.password.AuthPlainPassword;
import pro.gravit.launcher.core.api.method.password.AuthTotpPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.User;
import pro.gravit.launcher.core.hasher.HashedDir;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RequestFeatureAPIImpl implements AuthFeatureAPI, UserFeatureAPI, ProfileFeatureAPI {
    private final RequestService request;
    private final String authId;

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
        return request.request(new AuthRequest(login, convertAuthPasswordAll(password), authId, false, AuthRequest.ConnectTypes.CLIENT))
                .thenApply(response -> new AuthResponse(response.makeUserInfo(), response.oauth));
    }

    private AuthRequest.AuthPasswordInterface convertAuthPasswordAll(AuthMethodPassword password) {
        AuthRequest.AuthPasswordInterface requestPassword;
        if(password instanceof AuthChainPassword chain) {
            if(chain.list().size() == 1) {
                requestPassword = convertAuthPassword(chain.list().get(0));
            } else if(chain.list().size() == 2) {
                requestPassword = new Auth2FAPassword(convertAuthPassword(chain.list().get(0)),
                        convertAuthPassword(chain.list().get(1)));
            } else {
                var multi = new AuthMultiPassword();
                for(var e : chain.list()) {
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
        if(password instanceof AuthPlainPassword plain) {
            String encryptKey = Launcher.getConfig().passwordEncryptKey;
            if(encryptKey != null) {
                try {
                    return new AuthAESPassword(SecurityHelper.encrypt(encryptKey, plain.value()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return new pro.gravit.launcher.base.request.auth.password.AuthPlainPassword(plain.value());
            }
        } else if(password instanceof AuthTotpPassword totp) {
            return new AuthTOTPPassword(totp.value());
        } else if(password instanceof AuthOAuthPassword oauth) {
            return new AuthCodePassword(oauth.redirectUrl());
        } else if(password instanceof AuthRequest.AuthPasswordInterface custom) {
            return custom;
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
            return e.userInfo;
        });
    }

    @Override
    public CompletableFuture<Void> exit() {
        return request.request(new ExitRequest()).thenApply(response -> null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public CompletableFuture<List<ProfileFeatureAPI.ClientProfile>> getProfiles() {
        return request.request(new ProfilesRequest()).thenApply(response -> (List) response.profiles);
    }

    @Override
    public CompletableFuture<UpdateInfo> fetchUpdateInfo(String dirName) {
        return request.request(new UpdateRequest(dirName)).thenApply(response -> new UpdateInfoData(response.hdir, response.url));
    }

    public record UpdateInfoData(HashedDir hdir, String url) implements ProfileFeatureAPI.UpdateInfo {}
}
