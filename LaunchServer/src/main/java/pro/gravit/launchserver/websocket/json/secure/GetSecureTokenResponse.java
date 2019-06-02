package pro.gravit.launchserver.websocket.json.secure;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.websocket.json.SimpleResponse;
import pro.gravit.launcher.events.request.GetSecureTokenRequestEvent;

public class GetSecureTokenResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "getSecureToken";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        String secureToken = LaunchServer.server.config.protectHandler.generateClientSecureToken();
        client.verifyToken = secureToken;
        sendResult(new GetSecureTokenRequestEvent(secureToken));
    }
}
