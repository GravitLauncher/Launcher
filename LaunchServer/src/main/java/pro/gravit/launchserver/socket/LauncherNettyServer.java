package pro.gravit.launchserver.socket;

import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.handlers.NettyIpForwardHandler;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.launchserver.socket.handlers.fileserver.FileServerHandler;
import pro.gravit.utils.BiHookSet;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

public class LauncherNettyServer implements AutoCloseable {
    public final ServerBootstrap serverBootstrap;
    public final EventLoopGroup bossGroup;
    public final EventLoopGroup workerGroup;
    public final WebSocketService service;
    public final BiHookSet<NettyConnectContext,SocketChannel> pipelineHook = new BiHookSet<>();
    private static final String WEBSOCKET_PATH = "/api";

    public LauncherNettyServer(LaunchServer server) {
        LaunchServer.NettyConfig config = server.config.netty;
        NettyObjectFactory.setUsingEpoll(config.performance.usingEpoll);
        if(config.performance.usingEpoll)
        {
            LogHelper.debug("Netty: Epoll enabled");
        }
        if(config.performance.usingEpoll && JVMHelper.OS_TYPE != JVMHelper.OS.LINUX)
        {
            LogHelper.error("netty,perfomance.usingEpoll work only Linux systems");
        }
        bossGroup = NettyObjectFactory.newEventLoopGroup(config.performance.bossThread);
        workerGroup = NettyObjectFactory.newEventLoopGroup(config.performance.workerThread);
        serverBootstrap = new ServerBootstrap();
        service = new WebSocketService(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), server);
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NettyObjectFactory.getServerSocketChannelClass())
                .handler(new LoggingHandler(config.logLevel))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        NettyConnectContext context = new NettyConnectContext();
                        //p.addLast(new LoggingHandler(LogLevel.INFO));
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        if (server.config.netty.ipForwarding)
                            pipeline.addLast(new NettyIpForwardHandler(context));
                        pipeline.addLast(new WebSocketServerCompressionHandler());
                        pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
                        if (server.config.netty.fileServerEnabled)
                            pipeline.addLast(new FileServerHandler(server.updatesDir, true));
                        pipeline.addLast(new WebSocketFrameHandler(context, server, service));
                        pipelineHook.hook(context, ch);
                    }
                });
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
