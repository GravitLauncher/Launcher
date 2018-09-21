package ru.gravit.launchserver.socket.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LogHelper.debug("New client %s", IOHelper.getIP(ctx.channel().remoteAddress()));
        channels.add(ctx.channel());
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // ping and pong frames already handled
        ByteBuf buf = frame.content();
        ByteBufInputStream input = new ByteBufInputStream(buf);
        long handshake = input.readLong();
        long connection_flags = input.readLong();
        long type = input.readInt();
        LogHelper.debug("MessageHead: handshake %dl, flags %dl, type %d", handshake,connection_flags,type);
    }
}
