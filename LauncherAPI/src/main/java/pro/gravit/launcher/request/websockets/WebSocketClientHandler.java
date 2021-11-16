package pro.gravit.launcher.request.websockets;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import pro.gravit.utils.helper.LogHelper;

import java.util.concurrent.TimeUnit;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handshaker;
    private final ClientJSONPoint clientJSONPoint;
    private ChannelPromise handshakeFuture;

    public WebSocketClientHandler(final WebSocketClientHandshaker handshaker, ClientJSONPoint clientJSONPoint) {
        this.handshaker = handshaker;
        this.clientJSONPoint = clientJSONPoint;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        handshaker.handshake(ctx.channel());
        clientJSONPoint.onOpen();
        ctx.executor().scheduleWithFixedDelay(() -> ctx.channel().writeAndFlush(new PingWebSocketFrame()), 20L, 20L, TimeUnit.SECONDS);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        //System.out.println("WebSocket Client disconnected!");
        clientJSONPoint.onDisconnect();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        final Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            // web socket client connected
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            handshakeFuture.setSuccess();
            return;
        }

        if (msg instanceof FullHttpResponse) {
            final FullHttpResponse response = (FullHttpResponse) msg;
            throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content="
                    + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        final WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            final TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            clientJSONPoint.onMessage(textFrame.text());
            if (LogHelper.isDevEnabled()) {
                LogHelper.dev("Message: %s", textFrame.text());
            }
            // uncomment to print request
            // logger.info(textFrame.text());
        } else if ((frame instanceof PingWebSocketFrame)) {
            frame.content().retain();
            ch.writeAndFlush(new PongWebSocketFrame(frame.content()), ch.voidPromise());
            //return;
        } else if (frame instanceof PongWebSocketFrame) {
        } else if (frame instanceof CloseWebSocketFrame)
            ch.close();
        else if (frame instanceof BinaryWebSocketFrame) {
            // uncomment to print request
            // logger.info(frame.content().toString());
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        } else {
            LogHelper.error(cause);
        }
        ctx.close();
    }
}