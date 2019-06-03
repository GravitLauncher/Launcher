package pro.gravit.launchserver.websocket.json.secure;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.VerifySecureTokenRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;

public class VerifySecureTokenResponse extends SimpleResponse {
    public String secureToken;

    @Override
    public String getType() {
        return "verifySecureToken";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        boolean success = LaunchServer.server.config.protectHandler.verifyClientSecureToken(secureToken, client.verifyToken);
        if (success) client.isSecure = true;
        sendResult(new VerifySecureTokenRequestEvent(success));
    }
}
