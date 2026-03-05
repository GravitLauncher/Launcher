package pro.gravit.launcher.base.request;

import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.core.api.features.*;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodDetails;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.method.details.AuthLoginOnlyDetails;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.User;
import pro.gravit.launcher.core.api.model.UserPermissions;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class RequestOfflineModeAPIImpl implements CoreFeatureAPI, AuthFeatureAPI, UserFeatureAPI, ProfileFeatureAPI, HardwareVerificationFeatureAPI {
    private AtomicReference<SelfUser> userRef = new AtomicReference<>();

    @Override
    public CompletableFuture<List<AuthMethod>> getAuthMethods() {
        return CompletableFuture.completedFuture(List.of(new OfflineAuthMethod()));
    }

    @Override
    public CompletableFuture<LauncherUpdateInfo> checkUpdates() {
        return CompletableFuture.completedFuture(new LauncherUpdateInfo(null, "1.0.0", false, false));
    }

    @Override
    public CompletableFuture<SelfUser> getCurrentUser() {
        if(userRef.get() == null) {
            return CompletableFuture.failedFuture(new RequestException("Not authorized"));
        }
        return CompletableFuture.completedFuture(userRef.get());
    }

    @Override
    public CompletableFuture<AuthResponse> auth(String login, AuthMethodPassword password) {
        userRef.set(new OfflineUser(login));
        return CompletableFuture.completedFuture(new AuthResponse(userRef.get(), new OfflineAuthData(login)));
    }

    @Override
    public CompletableFuture<AuthToken> refreshToken(String refreshToken) {
        return CompletableFuture.completedFuture(new OfflineAuthData(refreshToken));
    }

    @Override
    public CompletableFuture<SelfUser> restore(String accessToken, boolean fetchUser) {
        userRef.set(new OfflineUser(accessToken));
        return CompletableFuture.completedFuture(userRef.get());
    }

    @Override
    public CompletableFuture<Void> exit() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<User> getUserByUsername(String username) {
        if(userRef.get() != null && userRef.get().getUsername().equals(username)) {
            return CompletableFuture.completedFuture(userRef.get());
        }
        return CompletableFuture.failedFuture(new RequestException("User not found"));
    }

    @Override
    public CompletableFuture<User> getUserByUUID(UUID uuid) {
        if(userRef.get() != null && userRef.get().getUUID().equals(uuid)) {
            return CompletableFuture.completedFuture(userRef.get());
        }
        return CompletableFuture.failedFuture(new RequestException("User not found"));
    }

    @Override
    public CompletableFuture<Void> joinServer(String username, String accessToken, String serverID) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> joinServer(UUID uuid, String accessToken, String serverID) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<CheckServerResponse> checkServer(String username, String serverID, boolean extended) {
        return CompletableFuture.completedFuture( new CheckServerResponse(new OfflineUser(username),
                null, null, Map.of()));
    }

    @Override
    public CompletableFuture<List<ClientProfile>> getProfiles() {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<ClientProfile> changeCurrentProfile(ClientProfile profile) {
        return CompletableFuture.completedFuture(profile);
    }

    @Override
    public CompletableFuture<UpdateInfo> fetchUpdateInfo(String dirName) {
        return CompletableFuture.failedFuture(new RequestException("Offline mode not supported this"));
    }

    @Override
    public CompletableFuture<SecurityLevelInfo> getSecurityInfo() {
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
        return CompletableFuture.failedFuture(new RequestException("Offline mode not support this"));
    }

    @Override
    public CompletableFuture<Void> sendHardwareInfo(HardwareStatisticData statisticData, HardwareIdentifyData identifyData) {
        return CompletableFuture.failedFuture(new RequestException("Offline mode not support this"));
    }

    public record OfflineAuthData(String login) implements AuthToken {

        @Override
        public String getAccessToken() {
            return login;
        }

        @Override
        public String getRefreshToken() {
            return login;
        }

        @Override
        public long getExpire() {
            return 0;
        }
    }

    public record OfflineUser(String login) implements SelfUser {

        @Override
        public String getAccessToken() {
            return login;
        }

        @Override
        public UserPermissions getPermissions() {
            return new ClientPermissions();
        }

        @Override
        public String getUsername() {
            return login;
        }

        @Override
        public UUID getUUID() {
            return UUID.fromString(login);
        }

        @Override
        public Map<String, Texture> getAssets() {
            return Map.of();
        }

        @Override
        public Map<String, String> getProperties() {
            return Map.of();
        }
    }

    public record OfflineAuthMethod() implements AuthMethod {

        @Override
        public List<AuthMethodDetails> getDetails() {
            return List.of(new AuthLoginOnlyDetails());
        }

        @Override
        public String getName() {
            return "offline";
        }

        @Override
        public String getDisplayName() {
            return "Offline Mode";
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
}
