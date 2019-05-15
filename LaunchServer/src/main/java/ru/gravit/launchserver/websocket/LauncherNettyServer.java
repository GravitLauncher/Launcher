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
import io.netty.handler.logging.LoggingHandler;
import ru.gravit.launcher.request.Request;
import ru.gravit.launcher.request.auth.AuthRequest;
import ru.gravit.launcher.request.websockets.StandartClientWebSocketService;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.websocket.fileserver.FileServerHandler;
import ru.gravit.utils.helper.LogHelper;

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
                .handler(new LoggingHandler(config.logLevel))
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    public void initChannel(NioSocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        NettyConnectContext context = new NettyConnectContext();
                        //p.addLast(new LoggingHandler(LogLevel.INFO));
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        if (LaunchServer.server.config.netty.ipForwarding)
                            pipeline.addLast(new NettyIpForwardHandler(context));
                        pipeline.addLast(new WebSocketServerCompressionHandler());
                        pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
                        if (LaunchServer.server.config.netty.fileServerEnabled)
                            pipeline.addLast(new FileServerHandler(LaunchServer.server.updatesDir, true));
                        pipeline.addLast(new WebSocketFrameHandler(context));
                    }
                });
        if (config.proxy != null && config.proxy.enabled) {
            LogHelper.info("Connect to main server %s");
            Request.service = StandartClientWebSocketService.initWebSockets(config.proxy.address, false);
            AuthRequest authRequest = new AuthRequest(config.proxy.login, config.proxy.password, config.proxy.auth_id, AuthRequest.ConnectTypes.PROXY);
            authRequest.initProxy = true;
            try {
                authRequest.request();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }
    }

    public ChannelFuture bind(InetSocketAddress address) {
        return serverBootstrap.bind(address);
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
