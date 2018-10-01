package ru.gravit.launchserver.socket.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public interface JsonResponse {
    String getType();
    void execute(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception;
}
