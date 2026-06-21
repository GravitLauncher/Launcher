package pro.gravit.launcher.core.api.features;

import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.model.SelfUser;

import java.util.concurrent.CompletableFuture;

public interface AuthFeatureAPI extends FeatureAPI {
    CompletableFuture<SelfUser> getCurrentUser();
    CompletableFuture<AuthResponse> auth(String login, AuthMethodPassword password);
    CompletableFuture<AuthToken> refreshToken(String refreshToken);
    CompletableFuture<SelfUser> restore(String accessToken, boolean fetchUser);
    default CompletableFuture<SelfUser> restoreFromExternal(AuthResponse external, boolean fetchUser) {
        return CompletableFuture.completedFuture(null);
    }
    CompletableFuture<Void> exit();

    record AuthResponse(SelfUser user, AuthToken authToken) {}

    interface AuthToken {
        String getAccessToken();
        String getRefreshToken();
        long getExpire();
    }
}
