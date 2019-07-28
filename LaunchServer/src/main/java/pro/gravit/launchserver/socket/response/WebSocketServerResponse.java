package pro.gravit.launchserver.socket.response;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.request.websockets.WebSocketRequest;
import pro.gravit.launchserver.socket.Client;

public interface WebSocketServerResponse extends WebSocketRequest {
    String getType();

    void execute(ChannelHandlerContext ctx, Client client) throws Exception;
}
