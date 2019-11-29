package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.hwid.HWID;
import pro.gravit.launcher.hwid.NoHWID;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthECPassword;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.hwid.HWIDException;
import pro.gravit.launchserver.auth.provider.AuthProvider;
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
import javax.crypto.IllegalBlockSizeException;
import java.security.SecureRandom;
import java.util.Collection;
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
    public HWID hwid;

    public enum ConnectTypes {
        SERVER, CLIENT, API
    }

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
            if (password instanceof AuthECPassword) {
                try {
                    password = new AuthPlainPassword(IOHelper.decode(SecurityHelper.decrypt(server.runtime.passwordEncryptKey
                            , ((AuthECPassword) password).password)));
                } catch (IllegalBlockSizeException | BadPaddingException ignored) {
                    throw new AuthException("Password decryption error");
                }
            }
            if(hwid == null) hwid = new NoHWID();
            AuthProviderPair pair;
            if (auth_id.isEmpty()) pair = server.config.getAuthProviderPair();
            else pair = server.config.getAuthProviderPair(auth_id);
            if(pair == null)
            {
                sendError("auth_id incorrect");
                return;
            }
            AuthContext context = new AuthContext(clientData, login, client, hwid, ip, authType);
            AuthProvider provider = pair.provider;
            server.authHookManager.preHook.hook(context, clientData);
            provider.preAuth(login, password, ip);
            AuthProviderResult aresult = provider.auth(login, password, ip);
            if (!VerifyHelper.isValidUsername(aresult.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", aresult.username));
                return;
            }
            Collection<ClientProfile> profiles = server.getProfiles();
            for (ClientProfile p : profiles) {
                if (p.getTitle().equals(client)) {
                    if (!p.isWhitelistContains(login)) {
                        throw new AuthException(server.config.whitelistRejectString);
                    }
                    clientData.profile = p;
                }
            }
            //if (clientData.profile == null) {
            //    throw new AuthException("You profile not found");
            //}
            if (authType == ConnectTypes.CLIENT)
                server.config.hwidHandler.check(hwid, aresult.username);
            server.authHookManager.postHook.hook(context, clientData);
            clientData.isAuth = true;
            clientData.permissions = aresult.permissions;
            clientData.auth_id = auth_id;
            clientData.updateAuth(server);
            if (result.playerProfile != null)
                clientData.username = result.playerProfile.username;
            else
                clientData.username = login;
            result.accessToken = aresult.accessToken;
            result.permissions = clientData.permissions;
            if (authType == ConnectTypes.SERVER && !clientData.permissions.canServer) {
                AuthProvider.authError("authType: SERVER not allowed for this account");
                return;
            }
            if (getSession) {
                if (clientData.session == 0) {
                    clientData.session = random.nextLong();
                    server.sessionManager.addClient(clientData);
                }
                result.session = clientData.session;
            }
            if (authType != ConnectTypes.API && server.config.protectHandler.allowGetAccessToken(context)) {
                UUID uuid = pair.handler.auth(aresult);
                result.playerProfile = ProfileByUUIDResponse.getProfile(uuid, aresult.username, client, clientData.auth.textureProvider);
                if (LogHelper.isDebugEnabled()) {
                    LogHelper.debug("Auth: %s accessToken %s uuid: %s", login, result.accessToken, uuid.toString());
                }
            }
            clientData.type = authType;
            sendResult(result);
        } catch (AuthException | HWIDException | HookException e) {
            sendError(e.getMessage());
        }
    }

    public static class AuthContext {
        public AuthContext(Client client, String login, String profileName, HWID hwid, String ip, ConnectTypes authType) {
            this.client = client;
            this.login = login;
            this.profileName = profileName;
            this.hwid = hwid;
            this.ip = ip;
            this.authType = authType;
        }

        public final String login;
        @Deprecated
        public int password_length; //Use AuthProvider for get password
        public final String profileName;
        public final HWID hwid;
        public final String ip;
        public final ConnectTypes authType;
        public final Client client;
    }
}
