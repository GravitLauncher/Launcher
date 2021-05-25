package pro.gravit.launchserver.socket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.socket.handlers.NettyIpForwardHandler;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.launchserver.socket.handlers.fileserver.FileServerHandler;
import pro.gravit.utils.BiHookSet;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class LauncherNettyServer implements AutoCloseable {
    private static final String WEBSOCKET_PATH = "/api";
    public final ServerBootstrap serverBootstrap;
    public final EventLoopGroup bossGroup;
    public final EventLoopGroup workerGroup;
    public final WebSocketService service;
    public final BiHookSet<NettyConnectContext, SocketChannel> pipelineHook = new BiHookSet<>();
    private transient final Logger logger = LogManager.getLogger();

    public LauncherNettyServer(LaunchServer server) {
        LaunchServerConfig.NettyConfig config = server.config.netty;
        NettyObjectFactory.setUsingEpoll(config.performance.usingEpoll);
        if (config.performance.usingEpoll) {
            logger.debug("Netty: Epoll enabled");
        }
        if (config.performance.usingEpoll && !Epoll.isAvailable()) {
            logger.error("Epoll is not available: (netty,perfomance.usingEpoll configured wrongly)", Epoll.unavailabilityCause());
        }
        bossGroup = NettyObjectFactory.newEventLoopGroup(config.performance.bossThread, "LauncherNettyServer.bossGroup");
        workerGroup = NettyObjectFactory.newEventLoopGroup(config.performance.workerThread, "LauncherNettyServer.workerGroup");
        serverBootstrap = new ServerBootstrap();
        service = new WebSocketService(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), server);
        serverBootstrap.group(bossGroup, workerGroup)
                .channelFactory(NettyObjectFactory.getServerSocketChannelFactory())
                .handler(new LoggingHandler(config.logLevel))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        NettyConnectContext context = new NettyConnectContext();
                        //p.addLast(new LoggingHandler(LogLevel.INFO));
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        pipeline.addLast("http-codec-compressor", new HttpObjectAggregator(server.config.netty.performance.maxWebSocketRequestBytes));
                        if (server.config.netty.ipForwarding)
                            pipeline.addLast("forward-http", new NettyIpForwardHandler(context));
                        pipeline.addLast("websock-comp", new WebSocketServerCompressionHandler());
                        pipeline.addLast("websock-codec", new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true, server.config.netty.performance.maxWebSocketRequestBytes));
                        if (!server.config.netty.disableWebApiInterface)
                            pipeline.addLast("webapi", new NettyWebAPIHandler(context));
                        if (server.config.netty.fileServerEnabled)
                            pipeline.addLast("fileserver", new FileServerHandler(server.updatesDir, true, config.showHiddenFiles));
                        pipeline.addLast("launchserver", new WebSocketFrameHandler(context, server, service));
                        pipelineHook.hook(context, ch);
                    }
                });
    }

    public ChannelFuture bind(InetSocketAddress address) {
        return serverBootstrap.bind(address);
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
        bossGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
    }
}
