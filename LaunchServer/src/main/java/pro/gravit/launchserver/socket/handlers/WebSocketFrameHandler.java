package pro.gravit.launchserver.socket.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.ScheduledFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.helper.IOHelper;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    public final LaunchServer srv;
    public final WebSocketService service;
    public final BiHookSet<ChannelHandlerContext, WebSocketFrame> hooks = new BiHookSet<>();
    private final UUID connectUUID = UUID.randomUUID();
    private transient final Logger logger = LogManager.getLogger();
    public NettyConnectContext context;
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
        this.client = client;
    }

    public final UUID getConnectUUID() {
        return connectUUID;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.trace("New client {}", IOHelper.getIP(ctx.channel().remoteAddress()));
        client = new Client();
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
            logger.error("WebSocket frame handler hook error", ex);
        }
        if (frame instanceof TextWebSocketFrame) {
            if(logger.isTraceEnabled()) {
                logger.trace("Message from {}: {}", context.ip == null ? IOHelper.getIP(ctx.channel().remoteAddress()) : context.ip, ((TextWebSocketFrame) frame).text());
            }
            try {
                service.process(ctx, (TextWebSocketFrame) frame, client, context.ip);
            } catch (Throwable ex) {
                logger.warn("Client {} send invalid request. Connection force closed.", context.ip == null ? IOHelper.getIP(ctx.channel().remoteAddress()) : context.ip);
                if (logger.isTraceEnabled()) {
                    logger.trace("Client message: {}", ((TextWebSocketFrame) frame).text());
                    logger.error("Process websockets request failed", ex);
                }
                ctx.channel().close();
            }
        } else if ((frame instanceof PingWebSocketFrame)) {
            frame.content().retain();
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
            //return;
        } else if ((frame instanceof PongWebSocketFrame)) {
            logger.trace("WebSocket Client received pong");
        } else if ((frame instanceof CloseWebSocketFrame)) {
            int statusCode = ((CloseWebSocketFrame) frame).statusCode();
            ctx.channel().close();
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            logger.error(new UnsupportedOperationException(message)); // prevent strange crash here.
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (future != null) future.cancel(true);
        if (logger.isTraceEnabled()) {
            logger.trace("Client {} disconnected", IOHelper.getIP(ctx.channel().remoteAddress()));
        }
        super.channelInactive(ctx);
    }
}
