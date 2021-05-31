package pro.gravit.launchserver.manangers;

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
import pro.gravit.launchserver.auth.core.AuthSocialProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportTextures;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderDAOResult;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import javax.crypto.Cipher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuthManager {
    private transient final LaunchServer server;
    private transient final Logger logger = LogManager.getLogger();

    public AuthManager(LaunchServer server) {
        this.server = server;
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
            AuthProvider.authError("Don't skip Launcher Update");
            return;
        }
        if (context.client.isAuth) {
            AuthProvider.authError("You are already logged in");
            return;
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
        AuthProviderPair pair = context.pair;
        AuthReport report;
        if (pair.core == null) {
            try {
                report = AuthReport.ofMinecraftAccessToken(authWithProviderAndHandler(context, password));
            } catch (Exception e) {
                if (e instanceof AuthException) throw (AuthException) e;
                throw new AuthException("Internal Auth Error. Please contact administrator");
            }
        } else {
            report = authWithCore(context, password);
        }
        return report;
    }

    @SuppressWarnings("deprecation")
    private String authWithProviderAndHandler(AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password) throws Exception {
        String accessToken;
        context.pair.provider.preAuth(context.login, password, context.ip);
        AuthProviderResult aresult = context.pair.provider.auth(context.login, password, context.ip);
        UUID uuid;
        String username = aresult.username != null ? aresult.username : context.login;
        if (aresult instanceof AuthProviderDAOResult) {
            context.client.daoObject = ((AuthProviderDAOResult) aresult).daoObject;
        }
        if (context.authType == AuthResponse.ConnectTypes.CLIENT && server.config.protectHandler.allowGetAccessToken(context)) {
            uuid = context.pair.handler.auth(aresult);
            accessToken = aresult.accessToken;
        } else {
            uuid = context.pair.handler.usernameToUUID(aresult.username);
            accessToken = null;
        }
        internalAuth(context.client, context.authType, context.pair, username, uuid, aresult.permissions, false);
        return accessToken;
    }

    private AuthReport authWithCore(AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password) throws AuthException {
        AuthCoreProvider provider = context.pair.core;
        provider.verifyAuth(context);
        if (password instanceof AuthOAuthPassword) {
            AuthOAuthPassword password1 = (AuthOAuthPassword) password;
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
                return AuthReport.ofMinecraftAccessToken(user.getAccessToken());
            }
            return AuthReport.ofMinecraftAccessToken(null);
        }
        User user = null;
        boolean skipPasswordCheck = false;
        String login = context.login;
        if (context.pair.social != null) {
            AuthSocialProvider.SocialResult result = context.pair.social.preAuth(context, password);
            if (result != null) {
                if (result.user != null) user = result.user;
                if (result.login != null) login = result.login;
                if (result.password != null) password = result.password;
                if (result.user != null && result.password == null) skipPasswordCheck = true;
            }
        }
        if (user == null && login != null) {
            user = provider.getUserByLogin(context.login);
            if (user == null) {
                throw new AuthException(AuthRequestEvent.USER_NOT_FOUND_ERROR_MESSAGE);
            }
        }
        AuthCoreProvider.PasswordVerifyReport report = null;
        if (!skipPasswordCheck) {
            report = provider.verifyPassword(user, password);
        }
        if (skipPasswordCheck || report.success) {
            AuthReport result;
            try {
                result = provider.createOAuthSession(user, context, report, context.authType == AuthResponse.ConnectTypes.CLIENT && server.config.protectHandler.allowGetAccessToken(context));
            } catch (IOException e) {
                if (e instanceof AuthException) throw (AuthException) e;
                logger.error(e);
                throw new AuthException("Internal Auth Error");
            }
            if (user == null) {
                if (result.session != null) {
                    user = result.session.getUser();
                } else {
                    logger.error("AuthCoreProvider {} method createOAuthSession returns null session with login null", context.pair.name);
                    throw new AuthException("Internal Auth Error");
                }
            }
            context.client.coreObject = user;
            internalAuth(context.client, context.authType, context.pair, user.getUsername(), user.getUUID(), user.getPermissions(), result.isUsingOAuth());
            return result;
        } else {
            if (report.needMoreFactor) {
                if (report.factors.size() == 1 && report.factors.get(0) == -1) {
                    throw new AuthException(AuthRequestEvent.TWO_FACTOR_NEED_ERROR_MESSAGE);
                }
                String message = AuthRequestEvent.ONE_FACTOR_NEED_ERROR_MESSAGE_PREFIX
                        .concat(report.factors.stream().map(String::valueOf).collect(Collectors.joining(".")));
                throw new AuthException(message);
            }
            throw new AuthException(AuthRequestEvent.WRONG_PASSWORD_ERROR_MESSAGE);
        }
    }

    /**
     * Writing authorization information to the Client object
     */
    public void internalAuth(Client client, AuthResponse.ConnectTypes authType, AuthProviderPair pair, String username, UUID uuid, ClientPermissions permissions, boolean oauth) {
        client.isAuth = true;
        client.permissions = permissions;
        client.auth_id = pair.name;
        client.auth = pair;
        client.username = username;
        client.type = authType;
        client.uuid = uuid;
        client.useOAuth = oauth;
        if (pair.isUseCore() && client.coreObject == null) {
            client.coreObject = pair.core.getUserByUUID(uuid);
        }
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

    public CheckServerReport checkServer(Client client, String username, String serverID) throws IOException {
        if (client.auth == null) return null;
        if (client.auth.isUseCore()) {
            User user = client.auth.core.checkServer(client, username, serverID);
            return user == null ? null : CheckServerReport.ofUser(user, getPlayerProfile(user));
        } else {
            UUID uuid = client.auth.handler.checkServer(username, serverID);
            return uuid == null ? null : CheckServerReport.ofUUID(uuid, getPlayerProfile(client.auth, uuid));
        }
    }

    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        if (client.auth == null) return false;
        if (client.auth.isUseCore()) {
            return client.auth.core.joinServer(client, username, accessToken, serverID);
        } else {
            return client.auth.handler.joinServer(username, accessToken, serverID);
        }
    }

    public PlayerProfile getPlayerProfile(Client client) {
        if (client.auth == null) return null;
        PlayerProfile playerProfile;
        if (client.useOAuth) {
            User user = client.getUser();
            playerProfile = getPlayerProfile(user);
            if (playerProfile != null) return playerProfile;
        }
        if (client.auth.textureProvider != null) {
            return getPlayerProfile(client.uuid, client.username, client.profile == null ? null : client.profile.getTitle(), client.auth.textureProvider);
        }
        // Return combined profile
        return new PlayerProfile(client.uuid, client.username, null, null);
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, String username) {
        return getPlayerProfile(pair, username, null);
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, String username, ClientProfile profile) {
        UUID uuid = null;
        if (pair.isUseCore()) {
            User user = pair.core.getUserByUsername(username);
            PlayerProfile playerProfile = getPlayerProfile(user);
            uuid = user.getUUID();
            if (playerProfile != null) return playerProfile;
        } else {
            try {
                uuid = pair.handler.usernameToUUID(username);
            } catch (IOException e) {
                logger.error("UsernameToUUID failed", e);
            }
        }
        if (uuid == null) {
            return null;
        }
        if (pair.textureProvider != null) {
            return getPlayerProfile(uuid, username, profile == null ? null : profile.getTitle(), pair.textureProvider);
        }
        return new PlayerProfile(uuid, username, null, null);
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, UUID uuid) {
        return getPlayerProfile(pair, uuid, null);
    }

    public PlayerProfile getPlayerProfile(AuthProviderPair pair, UUID uuid, ClientProfile profile) {
        String username = null;
        if (pair.isUseCore()) {
            User user = pair.core.getUserByUUID(uuid);
            PlayerProfile playerProfile = getPlayerProfile(user);
            username = user.getUsername();
            if (playerProfile != null) return playerProfile;
        } else {
            try {
                username = pair.handler.uuidToUsername(uuid);
            } catch (IOException e) {
                logger.error("UUIDToUsername failed", e);
            }
        }
        if (username == null) {
            return null;
        }
        if (pair.textureProvider != null) {
            return getPlayerProfile(uuid, username, profile == null ? null : profile.getTitle(), pair.textureProvider);
        }
        return new PlayerProfile(uuid, username, null, null);
    }

    public PlayerProfile getPlayerProfile(User user) {
        if (user instanceof UserSupportTextures) {
            return new PlayerProfile(user.getUUID(), user.getUsername(), ((UserSupportTextures) user).getSkinTexture(), ((UserSupportTextures) user).getCloakTexture());
        }
        return null;
    }

    private PlayerProfile getPlayerProfile(UUID uuid, String username, String client, TextureProvider textureProvider) {
        // Get skin texture
        Texture skin;
        try {
            skin = textureProvider.getSkinTexture(uuid, username, client);
        } catch (IOException e) {
            skin = null;
        }

        // Get cloak texture
        Texture cloak;
        try {
            cloak = textureProvider.getCloakTexture(uuid, username, client);
        } catch (IOException e) {
            cloak = null;
        }

        // Return combined profile
        return new PlayerProfile(uuid, username, skin, cloak);
    }

    public AuthRequest.AuthPasswordInterface decryptPassword(AuthRequest.AuthPasswordInterface password) throws AuthException {
        if (password instanceof Auth2FAPassword) {
            Auth2FAPassword auth2FAPassword = (Auth2FAPassword) password;
            auth2FAPassword.firstPassword = tryDecryptPasswordPlain(auth2FAPassword.firstPassword);
            auth2FAPassword.secondPassword = tryDecryptPasswordPlain(auth2FAPassword.secondPassword);
        } else if (password instanceof AuthMultiPassword) {
            AuthMultiPassword multiPassword = (AuthMultiPassword) password;
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

    @SuppressWarnings("deprecation")
    private AuthRequest.AuthPasswordInterface tryDecryptPasswordPlain(AuthRequest.AuthPasswordInterface password) throws AuthException {
        if (password instanceof AuthECPassword) {
            try {
                return new AuthPlainPassword(IOHelper.decode(SecurityHelper.decrypt(server.runtime.passwordEncryptKey
                        , ((AuthECPassword) password).password)));
            } catch (Exception ignored) {
                throw new AuthException("Password decryption error");
            }
        }
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

    public static class AuthReport {
        public final String minecraftAccessToken;
        public final String oauthAccessToken;
        public final String oauthRefreshToken;
        public final long oauthExpire;
        public final UserSession session;

        public AuthReport(String minecraftAccessToken, String oauthAccessToken, String oauthRefreshToken, long oauthExpire, UserSession session) {
            this.minecraftAccessToken = minecraftAccessToken;
            this.oauthAccessToken = oauthAccessToken;
            this.oauthRefreshToken = oauthRefreshToken;
            this.oauthExpire = oauthExpire;
            this.session = session;
        }

        public static AuthReport ofOAuth(String oauthAccessToken, String oauthRefreshToken, long oauthExpire) {
            return new AuthReport(null, oauthAccessToken, oauthRefreshToken, oauthExpire, null);
        }

        public static AuthReport ofOAuth(String oauthAccessToken, String oauthRefreshToken, long oauthExpire, UserSession session) {
            return new AuthReport(null, oauthAccessToken, oauthRefreshToken, oauthExpire, session);
        }

        public static AuthReport ofOAuthWithMinecraft(String minecraftAccessToken, String oauthAccessToken, String oauthRefreshToken, long oauthExpire) {
            return new AuthReport(minecraftAccessToken, oauthAccessToken, oauthRefreshToken, oauthExpire, null);
        }

        public static AuthReport ofOAuthWithMinecraft(String minecraftAccessToken, String oauthAccessToken, String oauthRefreshToken, long oauthExpire, UserSession session) {
            return new AuthReport(minecraftAccessToken, oauthAccessToken, oauthRefreshToken, oauthExpire, session);
        }

        public static AuthReport ofMinecraftAccessToken(String minecraftAccessToken) {
            return new AuthReport(minecraftAccessToken, null, null, 0, null);
        }

        public boolean isUsingOAuth() {
            return oauthAccessToken != null || oauthRefreshToken != null;
        }
    }
}
