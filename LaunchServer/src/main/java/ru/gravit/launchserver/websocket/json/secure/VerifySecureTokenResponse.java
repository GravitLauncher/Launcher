package ru.gravit.launchserver.websocket.json.secure;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.VerifySecureTokenRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;

public class VerifySecureTokenResponse extends SimpleResponse {
    public String secureToken;

    @Override
    public String getType() {
        return "verifySecureToken";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        boolean success = LaunchServer.server.config.protectHandler.verifyClientSecureToken(secureToken, client.verifyToken);
        if(success) client.isSecure = true;
        sendResult(new VerifySecureTokenRequestEvent(success));
    }
}
