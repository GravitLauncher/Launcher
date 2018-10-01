package ru.gravit.launchserver.socket.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import ru.gravit.utils.helper.LogHelper;

public class EchoResponse implements JsonResponse {
    public final String echo;

    public EchoResponse(String echo) {
        this.echo = echo;
    }

    @Override
    public String getType() {
        return "echo";
    }

    @Override
    public void execute(ChannelHandlerContext ctx, WebSocketFrame frame) {
        LogHelper.info("Echo: %s",echo);
    }
}
