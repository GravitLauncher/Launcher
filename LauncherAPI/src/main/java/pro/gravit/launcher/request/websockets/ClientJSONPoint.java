package pro.gravit.launcher.request.websockets;

import java.io.IOException;
import java.net.URI;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import pro.gravit.utils.helper.LogHelper;

public abstract class ClientJSONPoint {

    private final URI uri;
    protected Channel ch;
    private static final EventLoopGroup group = new NioEventLoopGroup();
    protected WebSocketClientHandler webSocketClientHandler;
    protected Bootstrap bootstrap = new Bootstrap();
    protected boolean ssl = false;
    protected int port;
    public boolean isClosed;

    public ClientJSONPoint(final String uri) throws SSLException {
        this(URI.create(uri));
    }

    public ClientJSONPoint(URI uri) throws SSLException {
        this.uri = uri;
        String protocol = uri.getScheme();
        if (!"ws".equals(protocol) && !"wss".equals(protocol)) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        if ("wss".equals(protocol)) {
            ssl = true;
        }
        if (uri.getPort() == -1) {
            if ("ws".equals(protocol)) port = 80;
            else port = 443;
        } else port = uri.getPort();
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().build();
        } else sslCtx = null;
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (sslCtx != null) {
                            pipeline.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), port));
                        }
                        pipeline.addLast("http-codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                        pipeline.addLast("ws-handler", webSocketClientHandler);
                    }
                });
    }

    public void open() throws Exception {
        //System.out.println("WebSocket Client connecting");
        webSocketClientHandler =
                new WebSocketClientHandler(
                        WebSocketClientHandshakerFactory.newHandshaker(
                                uri, WebSocketVersion.V13, null, false, EmptyHttpHeaders.INSTANCE, 12800000), this);
        ch = bootstrap.connect(uri.getHost(), port).sync().channel();
        webSocketClientHandler.handshakeFuture().sync();
    }

    public ChannelFuture send(String text) {
        LogHelper.dev("Send: %s", text);
        return ch.writeAndFlush(new TextWebSocketFrame(text), ch.voidPromise());
    }

    abstract void onMessage(String message) throws Exception;

    abstract void onDisconnect() throws Exception;

    abstract void onOpen() throws Exception;

    public void close() throws InterruptedException {
        //System.out.println("WebSocket Client sending close");
        isClosed = true;
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(new CloseWebSocketFrame(), ch.voidPromise());
            ch.closeFuture().sync();
        }

        //group.shutdownGracefully();
    }

    public void eval(final String text) throws IOException {
        ch.writeAndFlush(new TextWebSocketFrame(text), ch.voidPromise());
    }

}