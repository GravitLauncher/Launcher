package ru.gravit.launcher.request.websockets;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class ClientJSONPoint {

    private final URI uri;
    private Channel ch;
    private static final EventLoopGroup group = new NioEventLoopGroup();
    protected WebSocketClientHandler webSocketClientHandler;

    public ClientJSONPoint(final String uri) {
        this.uri = URI.create(uri);
    }

    public ClientJSONPoint(URI uri) {
        this.uri = uri;
    }

    public void open() throws Exception {
        Bootstrap b = new Bootstrap();
        String protocol = uri.getScheme();
        if (!"ws".equals(protocol) && !"wss".equals(protocol)) {
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
        boolean ssl = false;
        if("wss".equals(protocol))
        {
            ssl = true;
        }
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().build();
        } else {
            sslCtx = null;
        }

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
        webSocketClientHandler =
                new WebSocketClientHandler(
                        WebSocketClientHandshakerFactory.newHandshaker(
                                uri, WebSocketVersion.V13, null, false, HttpHeaders.EMPTY_HEADERS, 1280000));

        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (sslCtx != null) {
                            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                        }
                        pipeline.addLast("http-codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                        pipeline.addLast("ws-handler", webSocketClientHandler);
                    }
                });

        //System.out.println("WebSocket Client connecting");
        ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
        webSocketClientHandler.handshakeFuture().sync();
    }
    public ChannelFuture send(String text)
    {
        LogHelper.dev("Send: %s", text);
        return ch.writeAndFlush(new TextWebSocketFrame(text));
    }

    public void close() throws InterruptedException {
        //System.out.println("WebSocket Client sending close");
        ch.writeAndFlush(new CloseWebSocketFrame());
        ch.closeFuture().sync();
        //group.shutdownGracefully();
    }

    public void eval(final String text) throws IOException {
        ch.writeAndFlush(new TextWebSocketFrame(text));
    }

}