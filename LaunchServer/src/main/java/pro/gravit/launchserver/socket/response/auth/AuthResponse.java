package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderDAOResult;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.profile.ProfileByUUIDResponse;
import pro.gravit.utils.HookException;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

public class AuthResponse extends SimpleResponse {
    public final transient static Random random = new SecureRandom();
    public String login;
    public String client;
    public boolean getSession;

    public AuthRequest.AuthPasswordInterface password;

    public String auth_id;
    public ConnectTypes authType;

    @Override
    public String getType() {
        return "auth";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client clientData) throws Exception {
        try {
            AuthRequestEvent result = new AuthRequestEvent();
            if ((authType == null || authType == ConnectTypes.CLIENT) && (clientData == null || !clientData.checkSign)) {
                AuthProvider.authError("Don't skip Launcher Update");
                return;
            }

            if (clientData.isAuth) {
                if (LogHelper.isDevEnabled()) {
                    LogHelper.warning("Client %s double auth", clientData.username == null ? ip : clientData.username);
                }
                sendError("You are already logged in");
                return;
            }
            AuthProviderPair pair;
            if (auth_id == null || auth_id.isEmpty()) pair = server.config.getAuthProviderPair();
            else pair = server.config.getAuthProviderPair(auth_id);
            if (pair == null) {
                sendError("auth_id incorrect");
                return;
            }
            AuthContext context = new AuthContext(clientData, login, client, ip, authType);
            AuthProvider provider = pair.provider;
            server.authHookManager.preHook.hook(context, clientData);
            provider.preAuth(login, password, ip);
            if(password instanceof Auth2FAPassword) {
                AuthPlainPassword first = decryptPassword(server, ((Auth2FAPassword) password).firstPassword);
                AuthPlainPassword second = decryptPassword(server, ((Auth2FAPassword) password).secondPassword);
                if(first != null) {
                    ((Auth2FAPassword) password).firstPassword = first;
                }
                if(second != null) {
                    ((Auth2FAPassword) password).secondPassword = second;
                }
            }
            else {
                AuthPlainPassword passwd = decryptPassword(server, password);
                if(passwd != null) {
                    password = passwd;
                }
            }
            AuthProviderResult aresult = provider.auth(login, password, ip);
            if (!VerifyHelper.isValidUsername(aresult.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", aresult.username));
                return;
            }
            server.authHookManager.postHook.hook(context, clientData);
            clientData.isAuth = true;
            clientData.permissions = aresult.permissions;
            clientData.auth_id = auth_id;
            clientData.updateAuth(server);
            if (aresult.username != null)
                clientData.username = aresult.username;
            else
                clientData.username = login;
            if (aresult instanceof AuthProviderDAOResult) {
                clientData.daoObject = ((AuthProviderDAOResult) aresult).daoObject;
            }
            result.accessToken = aresult.accessToken;
            result.permissions = clientData.permissions;
            if (getSession) {
                if (clientData.session == null) {
                    clientData.session = UUID.randomUUID();
                    //server.sessionManager.addClient(clientData);
                }
                result.session = clientData.session;
            }
            if (authType == ConnectTypes.CLIENT && server.config.protectHandler.allowGetAccessToken(context)) {
                clientData.uuid = pair.handler.auth(aresult);
                if (LogHelper.isDebugEnabled()) {
                    LogHelper.debug("Auth: %s accessToken %s uuid: %s", login, result.accessToken, clientData.uuid.toString());
                }
            } else {
                clientData.uuid = pair.handler.usernameToUUID(aresult.username);
                result.accessToken = null;
            }

            result.playerProfile = ProfileByUUIDResponse.getProfile(clientData.uuid, aresult.username, client, clientData.auth.textureProvider);

            clientData.type = authType;
            sendResult(result);
        } catch (AuthException | HookException e) {
            sendError(e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    public static AuthPlainPassword decryptPassword(LaunchServer server, AuthRequest.AuthPasswordInterface password) throws Exception {
        if (password instanceof AuthECPassword) {
            try {
                return new AuthPlainPassword(IOHelper.decode(SecurityHelper.decrypt(server.runtime.passwordEncryptKey
                        , ((AuthECPassword) password).password)));
            } catch (IllegalBlockSizeException | BadPaddingException ignored) {
                throw new AuthException("Password decryption error");
            }
        }
        if (password instanceof AuthAESPassword) {
            try {
                return new AuthPlainPassword(IOHelper.decode(SecurityHelper.decrypt(server.runtime.passwordEncryptKey
                        , ((AuthAESPassword) password).password)));
            } catch (IllegalBlockSizeException | BadPaddingException ignored) {
                throw new AuthException("Password decryption error");
            }
        }
        if(password instanceof AuthRSAPassword) {
            try {
                Cipher cipher = SecurityHelper.newRSADecryptCipher(server.keyAgreementManager.rsaPrivateKey);
                return new AuthPlainPassword(
                        IOHelper.decode(cipher.doFinal(((AuthRSAPassword) password).password))
                );
            } catch (IllegalBlockSizeException | BadPaddingException ignored) {
                throw new AuthException("Password decryption error");
            }
        }
        return null;
    }

    public enum ConnectTypes {
        @Deprecated
        SERVER,
        CLIENT,
        API
    }

    public static class AuthContext {
        public final String login;
        public final String profileName;
        public final String ip;
        public final ConnectTypes authType;
        public final Client client;
        @Deprecated
        public int password_length; //Use AuthProvider for get password

        public AuthContext(Client client, String login, String profileName, String ip, ConnectTypes authType) {
            this.client = client;
            this.login = login;
            this.profileName = profileName;
            this.ip = ip;
            this.authType = authType;
        }
    }
}
