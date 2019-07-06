package pro.gravit.launchserver.socket.response.secure;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.events.request.VerifySecureTokenRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;

public class VerifySecureTokenResponse extends SimpleResponse {
    public String secureToken;

    @Override
    public String getType() {
        return "verifySecureToken";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        boolean success = server.config.protectHandler.verifyClientSecureToken(secureToken, client.verifyToken);
        if (success) client.isSecure = true;
        sendResult(new VerifySecureTokenRequestEvent(success));
    }
}
