package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.RestoreRequestEvent;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

import java.util.HashMap;
import java.util.Map;

public class RestoreResponse extends SimpleResponse {
    @FunctionalInterface
    public interface ExtendedTokenProvider {
        void accept(Client client, AuthProviderPair pair, String extendedToken);
    }
    public static Map<String, ExtendedTokenProvider> providers = new HashMap<>();
    public String authId;
    public String accessToken;
    public Map<String, String> extended;
    public boolean needUserInfo;

    @Override
    public String getType() {
        return "restore";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        if(accessToken == null && !client.isAuth && needUserInfo) {
            sendError("Invalid request");
            return;
        }
        AuthProviderPair pair;
        if(!client.isAuth) {
            if(authId == null || !client.useOAuth) {
                pair = server.config.getAuthProviderPair();
            } else {
                pair = client.auth;
            }
        } else {
            pair = server.config.getAuthProviderPair(authId);
        }
        if(pair == null || !pair.isUseCore()) {
            sendError("Invalid request");
            return;
        }
        if(accessToken != null) {
            UserSession session;
            try {
                session = pair.core.getUserSessionByOAuthAccessToken(accessToken);
            } catch (AuthCoreProvider.OAuthAccessTokenExpired e) {
                sendError(AuthRequestEvent.OAUTH_TOKEN_EXPIRE);
                return;
            }
            if(session == null) {
                sendError(AuthRequestEvent.OAUTH_TOKEN_INVALID);
                return;
            }
            User user = session.getUser();
            client.coreObject = user;
            client.sessionObject = session;
            server.authManager.internalAuth(client, client.type == null ? AuthResponse.ConnectTypes.API : client.type, pair, user.getUsername(), user.getUUID(), user.getPermissions(), true);
        }
        if(extended != null) {
            extended.forEach((k,v) -> {
                ExtendedTokenProvider provider = providers.get(k);
                if(provider == null) return;
                provider.accept(client, pair, v);
            });
        }
        if(needUserInfo && client.isAuth) {
            sendResult(new RestoreRequestEvent(CurrentUserResponse.collectUserInfoFromClient(client)));
        } else {
            sendResult(new RestoreRequestEvent());
        }
    }
}
