package ru.gravit.launchserver.websocket.json.secure;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.GetSecureTokenRequestEvent;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.websocket.WebSocketService;
import ru.gravit.launchserver.websocket.json.JsonResponseInterface;

public class GetSecureTokenResponse implements JsonResponseInterface {
    @Override
    public String getType() {
        return "getSecureToken";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) throws Exception {
        String secureToken = LaunchServer.server.config.protectHandler.generateClientSecureToken();
        service.sendObject(ctx, new GetSecureTokenRequestEvent(secureToken));
    }
}
