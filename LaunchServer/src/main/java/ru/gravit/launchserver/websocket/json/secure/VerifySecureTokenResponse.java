package ru.gravit.launchserver.websocket.json.secure;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.VerifySecureTokenRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.WebSocketService;
import ru.gravit.launchserver.websocket.json.JsonResponseInterface;

public class VerifySecureTokenResponse implements JsonResponseInterface {
    public String secureToken;

    @Override
    public String getType() {
        return "verifySecureToken";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        boolean success = LaunchServer.server.config.protectHandler.verifyClientSecureToken(secureToken);
        if(success) client.isSecure = true;
        service.sendObject(ctx, new VerifySecureTokenRequestEvent(success));
    }
}
