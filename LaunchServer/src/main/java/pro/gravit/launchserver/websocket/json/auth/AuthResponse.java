package pro.gravit.launchserver.websocket.json.auth;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.OshiHWID;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.hwid.HWIDException;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;
import pro.gravit.launchserver.websocket.json.profile.ProfileByUUIDResponse;
import pro.gravit.utils.HookException;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.utils.helper.VerifyHelper;

public class AuthResponse extends SimpleResponse {
    public transient static Random random = new SecureRandom();
    public String login;
    public String client;
    public String customText;
    public boolean getSession;

    public String password;
    public byte[] encryptedPassword;

    public AuthResponse(String login, String password, String auth_id, OshiHWID hwid) {
        this.login = login;
        this.password = password;
        this.auth_id = auth_id;
        this.hwid = hwid;
    }

    public String auth_id;
    public boolean initProxy;
    public ConnectTypes authType;
    public OshiHWID hwid;

    public enum ConnectTypes {
        SERVER, CLIENT, BOT
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
            if (password == null) {
                try {
                    password = IOHelper.decode(SecurityHelper.newRSADecryptCipher(LaunchServer.server.privateKey).
                            doFinal(encryptedPassword));
                } catch (IllegalBlockSizeException | BadPaddingException ignored) {
                    throw new AuthException("Password decryption error");
                }
            }
            clientData.permissions = LaunchServer.server.config.permissionsHandler.getPermissions(login);
            if (authType == ConnectTypes.BOT && !clientData.permissions.canBot) {
                AuthProvider.authError("authType: BOT not allowed for this account");
            }
            if (authType == ConnectTypes.SERVER && !clientData.permissions.canServer) {
                AuthProvider.authError("authType: SERVER not allowed for this account");
            }
            AuthProviderPair pair;
            if (auth_id.isEmpty()) pair = LaunchServer.server.config.getAuthProviderPair();
            else pair = LaunchServer.server.config.getAuthProviderPair(auth_id);
            AuthContext context = new AuthContext(0, login, password.length(), customText, client, ip, null, false);
            AuthProvider provider = pair.provider;
            LaunchServer.server.authHookManager.preHook.hook(context, clientData);
            provider.preAuth(login, password, customText, ip);
            AuthProviderResult aresult = provider.auth(login, password, ip);
            if (!VerifyHelper.isValidUsername(aresult.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", aresult.username));
                return;
            }
            Collection<ClientProfile> profiles = LaunchServer.server.getProfiles();
            for (ClientProfile p : profiles) {
                if (p.getTitle().equals(client)) {
                    if (!p.isWhitelistContains(login)) {
                        throw new AuthException(LaunchServer.server.config.whitelistRejectString);
                    }
                    clientData.profile = p;
                }
            }
            //if (clientData.profile == null) {
            //    throw new AuthException("You profile not found");
            //}
            if (authType == ConnectTypes.CLIENT)
                LaunchServer.server.config.hwidHandler.check(hwid, aresult.username);
            LaunchServer.server.authHookManager.postHook.hook(context, clientData);
            clientData.isAuth = true;
            clientData.permissions = aresult.permissions;
            clientData.auth_id = auth_id;
            clientData.updateAuth();
            result.accessToken = aresult.accessToken;
            result.permissions = clientData.permissions;
            if (getSession) {
                if (clientData.session == 0) {
                    clientData.session = random.nextLong();
                    LaunchServer.server.sessionManager.addClient(clientData);
                }
                result.session = clientData.session;
            }
            if (initProxy) {
                if (!clientData.permissions.canProxy) throw new AuthException("initProxy not allow");
                clientData.proxy = true;
            }
            if (LaunchServer.server.config.protectHandler.allowGetAccessToken(context)) {
                UUID uuid = pair.handler.auth(aresult);
                result.playerProfile = ProfileByUUIDResponse.getProfile(LaunchServer.server, uuid, aresult.username, client, clientData.auth.textureProvider);
                LogHelper.debug("Auth: %s accessToken %s uuid: %s", login, result.accessToken, uuid.toString());
            }
            sendResult(result);
        } catch (AuthException | HWIDException | HookException e) {
            sendError(e.getMessage());
        }
    }

    public static class AuthContext {
        public AuthContext(long session, String login, int password_lenght, String customText, String client, String hwid, String ip, boolean isServerAuth) {
            this.session = session;
            this.login = login;
            this.password_lenght = password_lenght;
            this.customText = customText;
            this.client = client;
            this.hwid = hwid;
            this.ip = ip;
            this.isServerAuth = isServerAuth;
        }

        public long session;
        public String login;
        public int password_lenght; //Use AuthProvider for get password
        public String client;
        public String hwid;
        public String customText;
        public String ip;
        public boolean isServerAuth;
    }
}
