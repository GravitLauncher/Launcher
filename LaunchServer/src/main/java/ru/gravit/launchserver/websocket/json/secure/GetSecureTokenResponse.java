package ru.gravit.launchserver.websocket.json.secure;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.GetSecureTokenRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.json.SimpleResponse;

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
