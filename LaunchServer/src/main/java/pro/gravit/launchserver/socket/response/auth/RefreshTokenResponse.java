package pro.gravit.launchserver.socket.response.auth;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.RefreshTokenRequestEvent;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class RefreshTokenResponse extends SimpleResponse {
    public String authId;
    public String refreshToken;

    @Override
    public String getType() {
        return "refreshToken";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) throws Exception {
        if (refreshToken == null) {
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
            sendError("Invalid request");
            return;
        }
        AuthManager.AuthReport report = pair.core.refreshAccessToken(refreshToken, new AuthResponse.AuthContext(client, null, null, ip, AuthResponse.ConnectTypes.API, pair));
        if (report == null || !report.isUsingOAuth()) {
            sendError("Invalid RefreshToken");
            return;
        }
        sendResult(new RefreshTokenRequestEvent(new AuthRequestEvent.OAuthRequestEvent(report.oauthAccessToken(), report.oauthRefreshToken(), report.oauthExpire())));
    }
}
