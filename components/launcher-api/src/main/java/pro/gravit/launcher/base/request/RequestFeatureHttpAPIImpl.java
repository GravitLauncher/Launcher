package pro.gravit.launcher.base.request;

import com.google.gson.JsonElement;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.helper.HttpHelper;
import pro.gravit.launcher.base.request.auth.password.AuthPlainPassword;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.method.password.AuthChainPassword;
import pro.gravit.launcher.core.api.method.password.AuthTotpPassword;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.api.model.User;
import pro.gravit.launcher.core.api.model.UserPermissions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class RequestFeatureHttpAPIImpl implements AuthFeatureAPI {
    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().build();
    private AtomicReference<HttpAuthData> authDataRef = new AtomicReference<>();

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

    public record HttpRefreshRequest(String refreshToken) {

    }
}
