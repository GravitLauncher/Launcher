package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private transient final Logger logger = LogManager.getLogger();
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
            AuthProviderPair pair;
            if (auth_id == null || auth_id.isEmpty()) pair = server.config.getAuthProviderPair();
            else pair = server.config.getAuthProviderPair(auth_id);
            if (pair == null) {
                sendError("auth_id incorrect");
                return;
            }
            AuthContext context = server.authManager.makeAuthContext(clientData, authType, pair, login, client, ip);
            server.authManager.check(context);
            password = server.authManager.decryptPassword(password);
            server.authHookManager.preHook.hook(context, clientData);
            server.authManager.auth(context, password);
            server.authHookManager.postHook.hook(context, clientData);
            if (getSession) {
                if (clientData.session == null) {
                    clientData.session = UUID.randomUUID();
                    //server.sessionManager.addClient(clientData);
                }
                result.session = clientData.session;
            }
            result.playerProfile = ProfileByUUIDResponse.getProfile(clientData.uuid, clientData.username, client, clientData.auth.textureProvider);
            sendResult(result);
        } catch (AuthException | HookException e) {
            sendError(e.getMessage());
        }
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
        public final AuthProviderPair pair;
        @Deprecated
        public int password_length; //Use AuthProvider for get password

        public AuthContext(Client client, String login, String profileName, String ip, ConnectTypes authType, AuthProviderPair pair) {
            this.client = client;
            this.login = login;
            this.profileName = profileName;
            this.ip = ip;
            this.authType = authType;
            this.pair = pair;
        }
    }
}
