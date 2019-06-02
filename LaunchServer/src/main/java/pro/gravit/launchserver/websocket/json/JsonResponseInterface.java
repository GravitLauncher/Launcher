package pro.gravit.launchserver.websocket.json;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.request.websockets.RequestInterface;
import pro.gravit.launchserver.socket.Client;

public interface JsonResponseInterface extends RequestInterface {
    String getType();

    void execute(ChannelHandlerContext ctx, Client client) throws Exception;
}
