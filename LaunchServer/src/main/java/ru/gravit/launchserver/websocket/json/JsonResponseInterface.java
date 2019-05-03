package ru.gravit.launchserver.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import ru.gravit.launcher.request.websockets.RequestInterface;
import ru.gravit.launchserver.socket.Client;

public interface JsonResponseInterface extends RequestInterface {
    String getType();

    void execute(ChannelHandlerContext ctx, Client client) throws Exception;
}
