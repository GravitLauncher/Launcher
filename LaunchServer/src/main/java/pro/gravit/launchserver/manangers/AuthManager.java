package pro.gravit.launchserver.manangers;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportProperties;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportTextures;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.auth.RestoreResponse;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import javax.crypto.Cipher;
import java.io.IOException;
import java.util.*;

public class AuthManager {
    private transient final LaunchServer server;
    private transient final Logger logger = LogManager.getLogger();
    private transient final JwtParser checkServerTokenParser;

    public AuthManager(LaunchServer server) {
        this.server = server;
        this.checkServerTokenParser = Jwts.parserBuilder()
                .requireIssuer("LaunchServer")
                .require("tokenType", "checkServer")
                .setSigningKey(server.keyAgreementManager.ecdsaPublicKey)
                .build();
    }

    public String newCheckServerToken(String serverName, String authId) {
        return Jwts.builder()
                .setIssuer("LaunchServer")
                .claim("serverName", serverName)
                .claim("authId", authId)
                .claim("tokenType", "checkServer")
                .signWith(server.keyAgreementManager.ecdsaPrivateKey)
                .compact();
    }

    public record CheckServerTokenInfo(String serverName, String authId) {
    }

    public CheckServerTokenInfo parseCheckServerToken(String token) {
        try {
            var jwt = checkServerTokenParser.parseClaimsJws(token).getBody();
            return new CheckServerTokenInfo(jwt.get("serverName", String.class), jwt.get("authId", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    public static class CheckServerVerifier implements RestoreResponse.ExtendedTokenProvider {
        private final LaunchServer server;

        public CheckServerVerifier(LaunchServer server) {
            this.server = server;
        }

        @Override
        public boolean accept(Client client, AuthProviderPair pair, String extendedToken) {
            var info = server.authManager.parseCheckServerToken(extendedToken);
            if(info == null) {
                return false;
            }
            client.auth_id = info.authId;
            client.auth = server.config.getAuthProviderPair(info.authId);
            if(client.permissions == null) client.permissions = new ClientPermissions();
            client.permissions.addPerm("launchserver.checkserver");
            client.permissions.addPerm(String.format("launchserver.profile.%s.show", info.serverName));
            client.setSerializableProperty("launchserver.serverName", info.serverName);
            return true;
        }
    }

    /**
     * Create AuthContext
     *
     * @return AuthContext instance
     */
    public AuthResponse.AuthContext makeAuthContext(Client client, AuthResponse.ConnectTypes authType, AuthProviderPair pair, String login, String profileName, String ip) {
        Objects.requireNonNull(client, "Client must be not null");
        Objects.requireNonNull(authType, "authType must be not null");
        Objects.requireNonNull(pair, "AuthProviderPair must be not null");
        return new AuthResponse.AuthContext(client, login, profileName, ip, authType, pair);
    }

    /**
     * Validate auth params ans state
     *
     * @param context Auth context
     * @throws AuthException auth not possible
     */
    public void check(AuthResponse.AuthContext context) throws AuthException {
        if (context.authType == AuthResponse.ConnectTypes.CLIENT && !context.client.checkSign) {
            throw new AuthException("Don't skip Launcher Update");
        }
        if (context.client.isAuth) {
            throw new AuthException("You are already logged in");
        }
    }

    /**
     * Full client authorization with password verification
     *
     * @param context  AuthContext
     * @param password User password
     * @return Access token
     */
    public AuthReport auth(AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password) throws AuthException {
        AuthCoreProvider provider = context.pair.core;
        provider.verifyAuth(context);
        if (password instanceof AuthOAuthPassword password1) {
            UserSession session;
            try {
                session = provider.getUserSessionByOAuthAccessToken(password1.accessToken);
            } catch (AuthCoreProvider.OAuthAccessTokenExpired oAuthAccessTokenExpired) {
                throw new AuthException(AuthRequestEvent.OAUTH_TOKEN_EXPIRE);
            }
            if (session == null) {
                throw new AuthException(AuthRequestEvent.OAUTH_TOKEN_INVALID);
            }
            User user = session.getUser();
            context.client.coreObject = user;
            context.client.sessionObject = session;
            internalAuth(context.client, context.authType, context.pair, user.getUsername(), user.getUUID(), user.getPermissions(), true);
            if (context.authType == AuthResponse.ConnectTypes.CLIENT && server.config.protectHandler.allowGetAccessToken(context)) {
                return AuthReport.ofMinecraftAccessToken(user.getAccessToken(), session);
            }
            return AuthReport.ofMinecraftAccessToken(null, session);
        }
        String login = context.login;
        try {
            AuthReport result = provider.authorize(login, context, password, context.authType == AuthResponse.ConnectTypes.CLIENT && server.config.protectHandler.allowGetAccessToken(context));
            if(result == null || result.session == null || result.session.getUser() == null) {
                logger.error("AuthCoreProvider {} method 'authorize' return null", context.pair.name);
                throw new AuthException("Internal Auth Error");
            }
            var session = result.session;
            var user = session.getUser();
            context.client.coreObject = user;
            context.client.sessionObject = session;
            internalAuth(context.client, context.authType, context.pair, user.getUsername(), user.getUUID(), user.getPermissions(), result.isUsingOAuth());
            return result;
        } catch (IOException e) {
            if (e instanceof AuthException) throw (AuthException) e;
            logger.error(e);
            throw new AuthException("Internal Auth Error");
        }
    }

    /**
     * Writing authorization information to the Client object
     */
    public void internalAuth(Client client, AuthResponse.ConnectTypes authType, AuthProviderPair pair, String username, UUID uuid, ClientPermissions permissions, boolean oauth) {
        if(!oauth) {
            throw new UnsupportedOperationException("Unsupported legacy session system");
        }
        client.isAuth = true;
        client.permissions = permissions;
        client.auth_id = pair.name;
        client.auth = pair;
        client.username = username;
        client.type = authType;
        client.uuid = uuid;
    }

    public CheckServerReport checkServer(Client client, String username, String serverID) throws IOException {
        if (client.auth == null) return null;
        User user = client.auth.core.checkServer(client, username, serverID);
        if (user == null) return null;
        else return CheckServerReport.ofUser(user, getPlayerProfile(client.auth, user));
    }

    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        if (client.auth == null) return false;
        return client.auth.core.joinServer(client, username, accessToken, serverID);
    }

    public PlayerProfile getPlayerProfile(Client client) {
        if (client.auth == null) return null;
        PlayerProfile playerProfile;
        User user = client.getUser();
        if (user == null) {
            return null;
        }
        playerProfile = getPlayerProfile(client.auth, user);
        if (playerProfile != null) return playerProfile;
        if (client.auth.textureProvider != null) {
            return getPlayerProfile(client.uuid, client.username, client.profile == null ? null : client.profile.getTitle(), client.auth.textureProvider, new HashMap<>());
        }
        // Return combined profile
        return new PlayerProfile(client.uuid, client.username, new HashMap<>(), new HashMap<>());
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, String username) {
        return getPlayerProfile(pair, username, null);
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, String username, ClientProfile profile) {
        UUID uuid;
        User user = pair.core.getUserByUsername(username);
        if (user == null) {
            return null;
        }
        PlayerProfile playerProfile = getPlayerProfile(pair, user);
        uuid = user.getUUID();
        if (playerProfile != null) return playerProfile;
        if (uuid == null) {
            return null;
        }
        if (pair.textureProvider != null) {
            return getPlayerProfile(uuid, username, profile == null ? null : profile.getTitle(), pair.textureProvider, new HashMap<>());
        }
        return new PlayerProfile(uuid, username, new HashMap<>(), new HashMap<>());
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, UUID uuid) {
        return getPlayerProfile(pair, uuid, null);
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, UUID uuid, ClientProfile profile) {
        String username;
        User user = pair.core.getUserByUUID(uuid);
        if (user == null) {
            return null;
        }
        PlayerProfile playerProfile = getPlayerProfile(pair, user);
        username = user.getUsername();
        if (playerProfile != null) return playerProfile;
        if (username == null) {
            return null;
        }
        if (pair.textureProvider != null) {
            return getPlayerProfile(uuid, username, profile == null ? null : profile.getTitle(), pair.textureProvider, new HashMap<>());
        }
        return new PlayerProfile(uuid, username, new HashMap<>(), new HashMap<>());
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, User user) {
        Map<String, String> properties;
        if(user instanceof UserSupportProperties userSupportProperties) {
            properties = userSupportProperties.getProperties();
        } else {
            properties = new HashMap<>();
        }
        if (user instanceof UserSupportTextures userSupportTextures) {
            return new PlayerProfile(user.getUUID(), user.getUsername(), userSupportTextures.getUserAssets(), properties);
        }
        if (pair.textureProvider == null) {
            throw new NullPointerException("TextureProvider not found");
        }
        return getPlayerProfile(user.getUUID(), user.getUsername(), "", pair.textureProvider, properties);
    }

    private PlayerProfile getPlayerProfile(UUID uuid, String username, String client, TextureProvider textureProvider, Map<String, String> properties) {
        // Get skin texture
        var assets = textureProvider.getAssets(uuid, username, client);

        // Return combined profile
        return new PlayerProfile(uuid, username, assets, properties);
    }

    public AuthRequest.AuthPasswordInterface decryptPassword(AuthRequest.AuthPasswordInterface password) throws AuthException {
        if (password instanceof Auth2FAPassword auth2FAPassword) {
            auth2FAPassword.firstPassword = tryDecryptPasswordPlain(auth2FAPassword.firstPassword);
            auth2FAPassword.secondPassword = tryDecryptPasswordPlain(auth2FAPassword.secondPassword);
        } else if (password instanceof AuthMultiPassword multiPassword) {
            List<AuthRequest.AuthPasswordInterface> list = new ArrayList<>(multiPassword.list.size());
            for (AuthRequest.AuthPasswordInterface p : multiPassword.list) {
                list.add(tryDecryptPasswordPlain(p));
            }
            multiPassword.list = list;
        } else {
            password = tryDecryptPasswordPlain(password);
        }
        return password;
    }

    private AuthRequest.AuthPasswordInterface tryDecryptPasswordPlain(AuthRequest.AuthPasswordInterface password) throws AuthException {
        if (password instanceof AuthAESPassword) {
            try {
                return new AuthPlainPassword(IOHelper.decode(SecurityHelper.decrypt(server.runtime.passwordEncryptKey
                        , ((AuthAESPassword) password).password)));
            } catch (Exception ignored) {
                throw new AuthException("Password decryption error");
            }
        }
        if (password instanceof AuthRSAPassword) {
            try {
                Cipher cipher = SecurityHelper.newRSADecryptCipher(server.keyAgreementManager.rsaPrivateKey);
                return new AuthPlainPassword(
                        IOHelper.decode(cipher.doFinal(((AuthRSAPassword) password).password))
                );
            } catch (Exception ignored) {
                throw new AuthException("Password decryption error");
            }
        }
        return password;
    }

    public static class CheckServerReport {
        public UUID uuid;
        public User user;
        public PlayerProfile playerProfile;

        public CheckServerReport(UUID uuid, User user, PlayerProfile playerProfile) {
            this.uuid = uuid;
            this.user = user;
            this.playerProfile = playerProfile;
        }

        public static CheckServerReport ofUser(User user, PlayerProfile playerProfile) {
            return new CheckServerReport(user.getUUID(), user, playerProfile);
        }

        public static CheckServerReport ofUUID(UUID uuid, PlayerProfile playerProfile) {
            return new CheckServerReport(uuid, null, playerProfile);
        }
    }

    public record AuthReport(String minecraftAccessToken, String oauthAccessToken,
                             String oauthRefreshToken, long oauthExpire,
                             UserSession session) {

        public static AuthReport ofOAuth(String oauthAccessToken, String oauthRefreshToken, long oauthExpire, UserSession session) {
            return new AuthReport(null, oauthAccessToken, oauthRefreshToken, oauthExpire, session);
        }

        public static AuthReport ofOAuthWithMinecraft(String minecraftAccessToken, String oauthAccessToken, String oauthRefreshToken, long oauthExpire, UserSession session) {
            return new AuthReport(minecraftAccessToken, oauthAccessToken, oauthRefreshToken, oauthExpire, session);
        }

        public static AuthReport ofMinecraftAccessToken(String minecraftAccessToken, UserSession session) {
            return new AuthReport(minecraftAccessToken, null, null, 0, session);
        }

        public boolean isUsingOAuth() {
            return oauthAccessToken != null || oauthRefreshToken != null;
        }
    }
}
