package ru.gravit.launchserver.websocket.json.auth;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.OshiHWID;
import ru.gravit.launcher.events.request.AuthRequestEvent;
import ru.gravit.launcher.events.request.ErrorRequestEvent;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.AuthProviderPair;
import ru.gravit.launchserver.auth.hwid.HWIDException;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.WebSocketService;
import ru.gravit.launchserver.websocket.json.JsonResponseInterface;
import ru.gravit.launchserver.websocket.json.profile.ProfileByUUIDResponse;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;

public class AuthResponse implements JsonResponseInterface {
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
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client clientData) throws Exception {
        try {
            AuthRequestEvent result = new AuthRequestEvent();
            String ip = IOHelper.getIP(ctx.channel().remoteAddress());
            if ((authType == null || authType == ConnectTypes.CLIENT) && !clientData.checkSign) {
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
            LaunchServer.server.authHookManager.preHook(context, clientData);
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
            LaunchServer.server.authHookManager.postHook(context, clientData);
            clientData.isAuth = true;
            clientData.permissions = aresult.permissions;
            clientData.auth_id = auth_id;
            clientData.updateAuth();
            result.accessToken = aresult.accessToken;
            result.permissions = clientData.permissions;
            if(getSession)
            {
                clientData.session = random.nextLong();
                LaunchServer.server.sessionManager.addClient(clientData);
                result.session = clientData.session;
            }
            if(LaunchServer.server.config.protectHandler.allowGetAccessToken(context))
            {
                UUID uuid = pair.handler.auth(aresult);
                result.playerProfile = ProfileByUUIDResponse.getProfile(LaunchServer.server, uuid, aresult.username, client, clientData.auth.textureProvider);
                LogHelper.debug("Auth: %s accessToken %s uuid: %s", login, result.accessToken, uuid.toString());
            }
            service.sendObject(ctx, result);
        } catch (AuthException | HWIDException e) {
            service.sendObject(ctx, new ErrorRequestEvent(e.getMessage()));
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
