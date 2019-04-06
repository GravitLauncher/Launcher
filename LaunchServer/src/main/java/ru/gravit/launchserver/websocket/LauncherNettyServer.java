package ru.gravit.launchserver.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.websocket.fileserver.FileServerHandler;

import java.net.InetSocketAddress;

public class LauncherNettyServer implements AutoCloseable {
    public final ServerBootstrap serverBootstrap;
    public final EventLoopGroup bossGroup;
    public final EventLoopGroup workerGroup;
    private static final String WEBSOCKET_PATH = "/api";

    public LauncherNettyServer() {
        LaunchServer.NettyConfig config = LaunchServer.server.config.netty;
        bossGroup = new NioEventLoopGroup(config.performance.bossThread);
        workerGroup = new NioEventLoopGroup(config.performance.workerThread);
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    public void initChannel(NioSocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        //p.addLast(new LoggingHandler(LogLevel.INFO));
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new WebSocketServerCompressionHandler());
                        pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
                        pipeline.addLast(new FileServerHandler(LaunchServer.server.updatesDir, true));
                        pipeline.addLast(new WebSocketFrameHandler());
                    }
                });
    }
    public ChannelFuture bind(InetSocketAddress address)
    {
        return serverBootstrap.bind(address);
    }

    @Override
    public void close() throws Exception {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
