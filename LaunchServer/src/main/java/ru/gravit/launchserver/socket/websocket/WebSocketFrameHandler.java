package ru.gravit.launchserver.socket.websocket;

import com.google.gson.GsonBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    public static LaunchServer server;
    public static GsonBuilder builder = new GsonBuilder();
    public static WebSocketService service = new WebSocketService(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), LaunchServer.server, builder);
    private Client client;

    static {
        service.registerResponses();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LogHelper.debug("New client %s", IOHelper.getIP(ctx.channel().remoteAddress()));
        client = new Client(0);
        service.registerClient(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // ping and pong frames already handled

        if (frame instanceof TextWebSocketFrame) {
            service.process(ctx, (TextWebSocketFrame) frame, client);
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }
}
