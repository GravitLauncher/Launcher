package pro.gravit.launchserver.auth.core.openid;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.base.request.auth.details.AuthWebViewDetails;
import pro.gravit.launcher.base.request.auth.password.AuthCodePassword;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.QueryHelper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class OpenIDAuthenticator {
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();
    private final OpenIDConfig openIDConfig;
    private final JwtParser jwtParser;

    public OpenIDAuthenticator(OpenIDConfig openIDConfig) {
        this.openIDConfig = openIDConfig;
        var keyLocator = loadKeyLocator(openIDConfig);
        this.jwtParser = Jwts.parser()
                .keyLocator(keyLocator)
                .requireIssuer(openIDConfig.issuer())
                .require("azp", openIDConfig.clientId())
                .build();
    }

    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails() {
        var state = UUID.randomUUID().toString();
        var uri = QueryBuilder.get(openIDConfig.authorizationEndpoint())
                .addQuery("response_type", "code")
                .addQuery("client_id", openIDConfig.clientId())
                .addQuery("redirect_uri", openIDConfig.redirectUri())
                .addQuery("scope", openIDConfig.scopes())
                .addQuery("state", state)
                .toUriString();

        return List.of(new AuthWebViewDetails(uri, openIDConfig.redirectUri()));
    }

    public TokenResponse refreshAccessToken(String oldRefreshToken) {
        var postBody = QueryBuilder.post()
                .addQuery("grant_type", "refresh_token")
                .addQuery("refresh_token", oldRefreshToken)
                .addQuery("client_id", openIDConfig.clientId())
                .addQuery("client_secret", openIDConfig.clientSecret())
                .toString();

        var accessTokenResponse = requestToken(postBody);
        var accessToken = accessTokenResponse.accessToken();
        var refreshToken = accessTokenResponse.refreshToken();

        try {
            readAndVerifyToken(accessToken);
        } catch (AuthException e) {
            throw new RuntimeException(e);
        }

        var accessTokenExpiresIn = Objects.requireNonNullElse(accessTokenResponse.expiresIn(), 0L);
        var refreshTokenExpiresIn = Objects.requireNonNullElse(accessTokenResponse.refreshExpiresIn(), 0L);

        return new TokenResponse(accessToken, accessTokenExpiresIn,
                refreshToken, refreshTokenExpiresIn);
    }

    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws AuthCoreProvider.OAuthAccessTokenExpired {
        Jws<Claims> token;
        try {
            token = readAndVerifyToken(accessToken);
        } catch (AuthException e) {
            throw new AuthCoreProvider.OAuthAccessTokenExpired("Can't read token", e);
        }
        var user = createUserFromToken(token);
        long expiresIn = 0;
        var expDate = token.getPayload().getExpiration();
        if (expDate != null) {
            expiresIn = expDate.toInstant().toEpochMilli();
        }

        return new OpenIDUserSession(user, accessToken, expiresIn);
    }

    public TokenResponse authorize(AuthCodePassword authCode) throws IOException {
        var uri = URI.create(authCode.uri);
        var queries = QueryHelper.splitUriQuery(uri);

        String code = CommonHelper.multimapFirstOrNullValue("code", queries);
        String error = CommonHelper.multimapFirstOrNullValue("error", queries);
        String errorDescription = CommonHelper.multimapFirstOrNullValue("error_description", queries);

        if (error != null && !error.isBlank()) {
            throw new AuthException("Auth error. Error: %s, description: %s".formatted(error, errorDescription));
        }


        var postBody = QueryBuilder.post()
                .addQuery("grant_type", "authorization_code")
                .addQuery("code", code)
                .addQuery("redirect_uri", openIDConfig.redirectUri())
                .addQuery("client_id", openIDConfig.clientId())
                .addQuery("client_secret", openIDConfig.clientSecret())
                .toString();

        var accessTokenResponse = requestToken(postBody);
        var accessToken = accessTokenResponse.accessToken();
        var refreshToken = accessTokenResponse.refreshToken();

        readAndVerifyToken(accessToken);

        var accessTokenExpiresIn = Objects.requireNonNullElse(accessTokenResponse.expiresIn(), 0L);
        var refreshTokenExpiresIn = Objects.requireNonNullElse(accessTokenResponse.refreshExpiresIn(), 0L);

        return new TokenResponse(accessToken, accessTokenExpiresIn,
                refreshToken, refreshTokenExpiresIn);
    }

    public User createUserFromToken(String accessToken) throws AuthException {
        return createUserFromToken(readAndVerifyToken(accessToken));
    }

    private Jws<Claims> readAndVerifyToken(String accessToken) throws AuthException {
        if (accessToken == null) {
            throw new AuthException("Token is null");
        }

        try {
            return jwtParser.parseSignedClaims(accessToken);
        } catch (JwtException e) {
            throw new AuthException("Bad token", e);
        }
    }

    private User createUserFromToken(Jws<Claims> token) {
        var username = token.getPayload().get(openIDConfig.extractorConfig().usernameClaim(), String.class);
        var uuidStr = token.getPayload().get(openIDConfig.extractorConfig().uuidClaim(), String.class);
        var uuid = UUID.fromString(uuidStr);
        return new UserEntity(username, uuid, new ClientPermissions());
    }

    private AccessTokenResponse requestToken(String postBody) {
        var request = HttpRequest.newBuilder()
                .uri(openIDConfig.tokenUri())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postBody))
                .build();

        HttpResponse<String> resp;
        try {
            resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Launcher.gsonManager.gson.fromJson(resp.body(), AccessTokenResponse.class);
    }

    private static KeyLocator loadKeyLocator(OpenIDConfig openIDConfig) {
        var request = HttpRequest.newBuilder(openIDConfig.jwksUri()).GET().build();
        HttpResponse<String> response;
        try {
            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        var jwks = Jwks.setParser().build().parse(response.body());
        return new KeyLocator(jwks);
    }

    private static class KeyLocator extends LocatorAdapter<Key> {
        private final Map<String, Key> keys;

        public KeyLocator(JwkSet jwks) {
            this.keys = jwks.getKeys().stream().collect(
                    Collectors.toMap(jwk -> String.valueOf(jwk.get("kid")), Jwk::toKey));
        }

        @Override
        protected Key locate(JweHeader header) {
            return super.locate(header);
        }

        @Override
        protected Key locate(JwsHeader header) {
            return keys.get(header.getKeyId());
        }

        @Override
        protected Key doLocate(Header header) {
            return super.doLocate(header);
        }
    }

    record OpenIDUserSession(User user, String token, long expiresIn) implements UserSession {
        @Override
        public String getID() {
            return user.getUsername();
        }

        @Override
        public User getUser() {
            return user;
        }

        @Override
        public String getMinecraftAccessToken() {
            return token;
        }

        @Override
        public long getExpireIn() {
            return expiresIn;
        }
    }
}
