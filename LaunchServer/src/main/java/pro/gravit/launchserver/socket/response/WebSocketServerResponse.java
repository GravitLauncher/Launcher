package pro.gravit.launchserver.socket.response;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.request.websockets.WebSocketRequest;
import pro.gravit.launchserver.socket.Client;

public interface WebSocketServerResponse extends WebSocketRequest {
    String getType();

    void execute(ChannelHandlerContext ctx, Client client) throws Exception;

    default ThreadSafeStatus getThreadSafeStatus() {
        return ThreadSafeStatus.READ;
    }

    enum ThreadSafeStatus {
        NONE, READ, READ_WRITE
    }
}
