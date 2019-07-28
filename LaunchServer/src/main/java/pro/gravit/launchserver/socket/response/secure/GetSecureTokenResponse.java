package pro.gravit.launchserver.socket.response.secure;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.GetSecureTokenRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class GetSecureTokenResponse extends SimpleResponse {
    @Override
    public String getType() {
        return "getSecureToken";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        String secureToken = server.config.protectHandler.generateClientSecureToken();
        client.verifyToken = secureToken;
        sendResult(new GetSecureTokenRequestEvent(secureToken));
    }
}
