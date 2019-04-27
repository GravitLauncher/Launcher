package ru.gravit.launchserver.websocket;

import com.google.gson.GsonBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.GlobalEventExecutor;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.util.concurrent.TimeUnit;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    public static LaunchServer server;
    public static GsonBuilder builder = new GsonBuilder();
    public static WebSocketService service = new WebSocketService(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), LaunchServer.server, builder);
    private Client client;

    static {
        service.registerResponses();
    }
    public void setClient(Client client)
    {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LogHelper.debug("New client %s", IOHelper.getIP(ctx.channel().remoteAddress()));
        client = new Client(0);
        service.registerClient(ctx.channel());
        ctx.executor().schedule(() -> {
            ctx.channel().writeAndFlush(new PingWebSocketFrame());
        }, 30L, TimeUnit.SECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // ping and pong frames already handled

        if (frame instanceof TextWebSocketFrame) {
            service.process(ctx, (TextWebSocketFrame) frame, client);
        } else if ((frame instanceof PingWebSocketFrame)) {
            frame.content().retain();
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
            //return;
        }
        else if ((frame instanceof PongWebSocketFrame)) {
            LogHelper.dev("WebSocket Client received pong");
        }
        else if ((frame instanceof CloseWebSocketFrame)) {
            ctx.channel().close();
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }
}
