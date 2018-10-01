package ru.gravit.launchserver.socket.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import ru.gravit.launchserver.socket.websocket.WebSocketService;

public interface JsonResponse {
    String getType();
    void execute(WebSocketService service,ChannelHandlerContext ctx) throws Exception;
}
