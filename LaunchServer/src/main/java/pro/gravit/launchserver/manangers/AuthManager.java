package pro.gravit.launchserver.manangers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderDAOResult;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
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

    public static class AuthReport {
        public final String minecraftAccessToken;
        public final String oauthAccessToken;
        public final String oauthRefreshToken;
        public final long oauthExpire;

        private AuthReport(String minecraftAccessToken, String oauthAccessToken, String oauthRefreshToken, long oauthExpire) {
            this.minecraftAccessToken = minecraftAccessToken;
            this.oauthAccessToken = oauthAccessToken;
            this.oauthRefreshToken = oauthRefreshToken;
            this.oauthExpire = oauthExpire;
        }

        public static AuthReport ofOAuth(String oauthAccessToken, String oauthRefreshToken, long oauthExpire) {
            return new AuthReport(null, oauthAccessToken, oauthRefreshToken, oauthExpire);
        }

        public static AuthReport ofOAuthWithMinecraft(String minecraftAccessToken, String oauthAccessToken, String oauthRefreshToken, long oauthExpire) {
            return new AuthReport(minecraftAccessToken, oauthAccessToken, oauthRefreshToken, oauthExpire);
        }

        public static AuthReport ofMinecraftAccessToken(String minecraftAccessToken) {
            return new AuthReport(minecraftAccessToken, null, null, 0);
        }

        public boolean isUsingOAuth() {
            return oauthAccessToken != null || oauthRefreshToken != null;
        }
    }

    /**
     * Full client authorization with password verification
     * @param context AuthContext
     * @param password User password
     * @return Access token
     */
    public AuthReport auth(AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password) throws AuthException {
        AuthProviderPair pair = context.pair;
        AuthReport report;
        if(pair.core == null) {
            try {
                report = AuthReport.ofMinecraftAccessToken(authWithProviderAndHandler(context, password));
            } catch (Exception e) {
                if(e instanceof AuthException) throw (AuthException) e;
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
        if(context.authType == AuthResponse.ConnectTypes.CLIENT && server.config.protectHandler.allowGetAccessToken(context)) {
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
        User user = provider.getUserByUsername(context.login);
        if(user == null) {
            throw new AuthException(AuthRequestEvent.USER_NOT_FOUND_ERROR_MESSAGE);
        }
        AuthCoreProvider.PasswordVerifyReport report = provider.verifyPassword(user, password);
        if(report.success) {
            UUID uuid = user.getUUID();
            AuthReport result;
            try {
                result = provider.createOAuthSession(user, context, report, context.authType == AuthResponse.ConnectTypes.CLIENT && server.config.protectHandler.allowGetAccessToken(context));
            } catch (IOException e) {
                logger.error(e);
                throw new AuthException("Internal Auth Error");
            }
            context.client.coreObject = user;
            internalAuth(context.client, context.authType, context.pair, user.getUsername(), uuid, user.getPermissions(), result.isUsingOAuth());
            return result;
        }
        else {
            if(report.needMoreFactor) {
                if(report.factors.size() == 1 && report.factors.get(0) == -1) {
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
        if(pair.isUseCore() && client.coreObject == null) {
            client.coreObject = pair.core.getUserByUUID(uuid);
        }
    }

    public UUID checkServer(Client client, String username, String serverID) throws IOException {
        if(client.auth == null) return null;
        if(client.auth.isUseCore()) {
            return client.auth.core.checkServer(client, username, serverID);
        }
        else {
            return client.auth.handler.checkServer(username, serverID);
        }
    }

    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        if(client.auth == null) return false;
        if(client.auth.isUseCore()) {
            return client.auth.core.joinServer(client, username, accessToken, serverID);
        } else {
            return client.auth.handler.joinServer(username, accessToken, serverID);
        }
    }

    public AuthRequest.AuthPasswordInterface decryptPassword(AuthRequest.AuthPasswordInterface password) throws AuthException {
        if(password instanceof Auth2FAPassword) {
            Auth2FAPassword auth2FAPassword = (Auth2FAPassword) password;
            auth2FAPassword.firstPassword = tryDecryptPasswordPlain(auth2FAPassword.firstPassword);
            auth2FAPassword.secondPassword = tryDecryptPasswordPlain(auth2FAPassword.secondPassword);
        }
        else if(password instanceof AuthMultiPassword) {
            AuthMultiPassword multiPassword = (AuthMultiPassword) password;
            List<AuthRequest.AuthPasswordInterface> list = new ArrayList<>(multiPassword.list.size());
            for(AuthRequest.AuthPasswordInterface p : multiPassword.list) {
                list.add(tryDecryptPasswordPlain(p));
            }
            multiPassword.list = list;
        }
        else {
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
        if(password instanceof AuthRSAPassword) {
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
}
