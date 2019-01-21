package ru.gravit.launchserver.socket.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.launchserver.socket.websocket.WebSocketService;

public class SimpleResponse implements JsonResponseInterface {
    @Override
    public String getType() {
        return null;
    }

    @Override
    public void execute(WebSocketService service, ChannelHandlerContext ctx, Client client) {

    }
}
