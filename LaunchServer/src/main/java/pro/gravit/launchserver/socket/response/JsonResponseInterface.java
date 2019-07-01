package pro.gravit.launchserver.socket.response;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.request.websockets.RequestInterface;
import pro.gravit.launchserver.socket.Client;

public interface JsonResponseInterface extends RequestInterface {
    String getType();

    void execute(ChannelHandlerContext ctx, Client client) throws Exception;
}
