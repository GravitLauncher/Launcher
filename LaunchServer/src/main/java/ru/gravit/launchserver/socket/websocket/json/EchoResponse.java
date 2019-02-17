package ru.gravit.launchserver.socket.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.events.request.EchoRequestEvent;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;
import ru.gravit.utils.helper.LogHelper;

public class EchoResponse implements JsonResponseInterface {
    public final String echo;

    public EchoResponse(String echo) {
        this.echo = echo;
    }

    @Override
    public String getType() {
        return "echo";
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) {
        LogHelper.info("Echo: %s, isAuth %s", echo, client.isAuth ? "true" : "false");
        service.sendObject(ctx, new EchoRequestEvent(echo));
    }

}
