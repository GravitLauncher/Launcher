package pro.gravit.launchserver.socket.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.ScheduledFuture;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    public final LaunchServer srv;
    public final WebSocketService service;
    private final UUID connectUUID = UUID.randomUUID();
    public NettyConnectContext context;
    public final BiHookSet<ChannelHandlerContext, WebSocketFrame> hooks = new BiHookSet<>();
    private Client client;
    private ScheduledFuture<?> future;

    public WebSocketFrameHandler(NettyConnectContext context, LaunchServer srv, WebSocketService service) {
        this.context = context;
        this.srv = srv;
        this.service = service;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        if(this.client != null) this.client.refCount.decrementAndGet();
        this.client = client;
        if(client != null) client.refCount.incrementAndGet();
    }

    public final UUID getConnectUUID() {
        return connectUUID;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (LogHelper.isDevEnabled()) {
            LogHelper.dev("New client %s", IOHelper.getIP(ctx.channel().remoteAddress()));
        }
        client = new Client(null);
        Channel ch = ctx.channel();
        service.registerClient(ch);
        future = ctx.executor().scheduleAtFixedRate(() -> ch.writeAndFlush(new PingWebSocketFrame(), ch.voidPromise()), 30L, 30L, TimeUnit.SECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // ping and pong frames already handled
        try {
            if (hooks.hook(ctx, frame)) return;
        } catch (Throwable ex) {
            LogHelper.error(ex);
        }
        if (frame instanceof TextWebSocketFrame) {
            try {
                service.process(ctx, (TextWebSocketFrame) frame, client, context.ip);
            } catch (Throwable ex) {
                if (LogHelper.isDebugEnabled()) {
                    LogHelper.warning("Client %s send invalid request. Connection force closed.", context.ip == null ? IOHelper.getIP(ctx.channel().remoteAddress()) : context.ip);
                    if (LogHelper.isDevEnabled()) {
                        LogHelper.dev("Client message: %s", ((TextWebSocketFrame) frame).text());
                    }
                    if (LogHelper.isStacktraceEnabled()) {
                        LogHelper.error(ex);
                    }
                }
                ctx.channel().close();
            }
        } else if ((frame instanceof PingWebSocketFrame)) {
            frame.content().retain();
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
            //return;
        } else if ((frame instanceof PongWebSocketFrame)) {
            LogHelper.dev("WebSocket Client received pong");
        } else if ((frame instanceof CloseWebSocketFrame)) {
            int statusCode = ((CloseWebSocketFrame) frame).statusCode();
            ctx.channel().close();
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            LogHelper.error(new UnsupportedOperationException(message)); // prevent strange crash here.
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (future != null) future.cancel(true);
        if(LogHelper.isDevEnabled()) {
            LogHelper.dev("Client %s disconnected", IOHelper.getIP(ctx.channel().remoteAddress()));
        }
        int refCount = client.refCount.decrementAndGet();
        if(client.session != null) {
            if(refCount == 0) {
                srv.sessionManager.addClient(client);
            }
            else if(refCount < 0) {
                LogHelper.warning("Client session %s reference counter invalid - %d", client.session, refCount);
            }
        }
        super.channelInactive(ctx);
    }
}
