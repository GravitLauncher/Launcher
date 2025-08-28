package pro.gravit.launchserver.socket.severlet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;

public class StatusSeverlet implements NettyWebAPIHandler.SimpleSeverletHandler {
    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest msg, NettyConnectContext context) {
        sendHttpResponse(ctx, new DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.OK));
    }
}
