package pro.gravit.launcher.request.websockets;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
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
import pro.gravit.launcher.CertificatePinningTrustManager;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.utils.helper.LogHelper;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class ClientJSONPoint {
    private static final AtomicInteger counter = new AtomicInteger();
    private static final ThreadFactory threadFactory = (runnable) -> {
        Thread t = new Thread(runnable);
        t.setName(String.format("Netty Thread #%d", counter.incrementAndGet()));
        t.setDaemon(true);
        return t;
    };
    private static final EventLoopGroup group = new NioEventLoopGroup(threadFactory);
    @LauncherInject("launcher.certificatePinning")
    private static boolean isCertificatePinning;
    protected final Bootstrap bootstrap = new Bootstrap();
    private final URI uri;
    public boolean isClosed;
    protected Channel ch;
    protected WebSocketClientHandler webSocketClientHandler;
    protected boolean ssl = false;
    protected int port;

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
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
            if (isCertificatePinning) {
                try {
                    sslContextBuilder.trustManager(CertificatePinningTrustManager.getTrustManager());
                } catch (KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException e) {
                    LogHelper.error(e);
                    sslContextBuilder.trustManager();
                }
            }
            sslCtx = sslContextBuilder.build();
        } else sslCtx = null;
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
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

    public void openAsync(Runnable onConnect, Consumer<Throwable> onFail) {
        //System.out.println("WebSocket Client connecting");
        webSocketClientHandler =
                new WebSocketClientHandler(
                        WebSocketClientHandshakerFactory.newHandshaker(
                                uri, WebSocketVersion.V13, null, false, EmptyHttpHeaders.INSTANCE, 12800000), this);
        ChannelFuture future = bootstrap.connect(uri.getHost(), port);
        future.addListener((l) -> {
            if(l.isSuccess()) {
                ch = future.channel();
                webSocketClientHandler.handshakeFuture().addListener((e) -> {
                    if(e.isSuccess()) {
                        onConnect.run();
                    } else {
                        onFail.accept(webSocketClientHandler.handshakeFuture().cause());
                    }
                });
            } else {
                onFail.accept(future.cause());
            }
        });
    }

    public ChannelFuture send(String text) {
        LogHelper.dev("Send: %s", text);
        return ch.writeAndFlush(new TextWebSocketFrame(text), ch.voidPromise());
    }

    abstract void onMessage(String message);

    abstract void onDisconnect();

    abstract void onOpen();

    public void close() throws InterruptedException {
        //System.out.println("WebSocket Client sending close");
        isClosed = true;
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(new CloseWebSocketFrame(), ch.voidPromise());
            ch.closeFuture().sync();
        }

        group.shutdownGracefully();
    }

    public void eval(final String text) {
        ch.writeAndFlush(new TextWebSocketFrame(text), ch.voidPromise());
    }

}