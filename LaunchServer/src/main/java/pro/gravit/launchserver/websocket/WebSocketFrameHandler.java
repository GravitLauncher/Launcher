package pro.gravit.launchserver.websocket;

import java.util.concurrent.TimeUnit;

import com.google.gson.GsonBuilder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    public static LaunchServer server;
    public static GsonBuilder builder = CommonHelper.newBuilder();
    public static WebSocketService service = new WebSocketService(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), LaunchServer.server, builder);
    public NettyConnectContext context;

    public WebSocketFrameHandler(NettyConnectContext context) {
        this.context = context;
    }

    private Client client;

    static {
        service.registerResponses();
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LogHelper.dev("New client %s", IOHelper.getIP(ctx.channel().remoteAddress()));
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
            service.process(ctx, (TextWebSocketFrame) frame, client, context.ip);
        } else if ((frame instanceof PingWebSocketFrame)) {
            frame.content().retain();
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
            //return;
        } else if ((frame instanceof PongWebSocketFrame)) {
            LogHelper.dev("WebSocket Client received pong");
        } else if ((frame instanceof CloseWebSocketFrame)) {
            ctx.channel().close();
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }
}
