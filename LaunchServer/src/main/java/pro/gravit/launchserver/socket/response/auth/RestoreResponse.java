package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.LauncherRequestEvent;
import pro.gravit.launcher.events.request.RestoreRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.protect.AdvancedProtectHandler;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import pro.gravit.launchserver.socket.response.update.LauncherResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestoreResponse extends SimpleResponse {
    public static Map<String, ExtendedTokenProvider> providers = new HashMap<>();
    private static boolean registeredProviders = false;
    public String authId;
    public String accessToken;
    public Map<String, String> extended;
    public boolean needUserInfo;

    public static void registerProviders(LaunchServer server) {
        if (!registeredProviders) {
            providers.put(LauncherRequestEvent.LAUNCHER_EXTENDED_TOKEN_NAME, new LauncherResponse.LauncherTokenVerifier(server));
            providers.put("publicKey", new AdvancedProtectHandler.PublicKeyTokenVerifier(server));
            providers.put("hardware", new AdvancedProtectHandler.HardwareInfoTokenVerifier(server));
            providers.put("checkServer", new AuthManager.CheckServerVerifier(server));
            registeredProviders = true;
        }
    }

    @Override
    public String getType() {
        return "restore";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        if (accessToken == null && !client.isAuth && needUserInfo) {
            sendError("Invalid request");
            return;
        }
        AuthProviderPair pair;
        if (!client.isAuth) {
            if (authId == null) {
                pair = server.config.getAuthProviderPair();
            } else {
                pair = server.config.getAuthProviderPair(authId);
            }
        } else {
            pair = client.auth;
        }
        if (pair == null) {
            sendError("Invalid authId");
            return;
        }
        if (accessToken != null) {
            UserSession session;
            try {
                session = pair.core.getUserSessionByOAuthAccessToken(accessToken);
            } catch (AuthCoreProvider.OAuthAccessTokenExpired e) {
                sendError(AuthRequestEvent.OAUTH_TOKEN_EXPIRE);
                return;
            }
            if (session == null) {
                sendError(AuthRequestEvent.OAUTH_TOKEN_INVALID);
                return;
            }
            User user = session.getUser();
            client.coreObject = user;
            client.sessionObject = session;
            server.authManager.internalAuth(client, client.type == null ? AuthResponse.ConnectTypes.API : client.type, pair, user.getUsername(), user.getUUID(), user.getPermissions(), true);
        }
        List<String> invalidTokens = new ArrayList<>(4);
        if (extended != null) {
            extended.forEach((k, v) -> {
                ExtendedTokenProvider provider = providers.get(k);
                if (provider == null) return;
                if (!provider.accept(client, pair, v)) {
                    invalidTokens.add(k);
                }
            });
        }
        if (needUserInfo && client.isAuth) {
            sendResult(new RestoreRequestEvent(CurrentUserResponse.collectUserInfoFromClient(server, client), invalidTokens));
        } else {
            sendResult(new RestoreRequestEvent(invalidTokens));
        }
    }

    @FunctionalInterface
    public interface ExtendedTokenProvider {
        boolean accept(Client client, AuthProviderPair pair, String extendedToken);
    }
}
