package pro.gravit.launchserver.socket.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.HookSet;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    static {
    }

    public final LaunchServer srv;
    public final WebSocketService service;
    private final UUID connectUUID = UUID.randomUUID();
    public NettyConnectContext context;
    public BiHookSet<ChannelHandlerContext, WebSocketFrame> hooks = new BiHookSet<>();
    private Client client;

    public WebSocketFrameHandler(NettyConnectContext context, LaunchServer srv, WebSocketService service) {
        this.context = context;
        this.srv = srv;
        this.service = service;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public final UUID getConnectUUID() {
        return connectUUID;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (LogHelper.isDevEnabled()) {
            LogHelper.dev("New client %s", IOHelper.getIP(ctx.channel().remoteAddress()));
        }
        client = new Client(0);
        Channel ch = ctx.channel();
        service.registerClient(ch);
        ctx.executor().schedule(() -> {
            ch.writeAndFlush(new PingWebSocketFrame(), ch.voidPromise());
        }, 30L, TimeUnit.SECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // ping and pong frames already handled
        if(hooks.hook(ctx, frame)) return;
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
            LogHelper.error(new UnsupportedOperationException(message)); // prevent strange crash here.
        }
    }
}
