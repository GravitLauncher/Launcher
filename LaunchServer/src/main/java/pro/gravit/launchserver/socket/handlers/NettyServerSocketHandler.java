package pro.gravit.launchserver.socket.handlers;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.socket.LauncherNettyServer;
import pro.gravit.utils.helper.LogHelper;

import javax.net.ssl.SSLServerSocketFactory;
import java.net.InetSocketAddress;

// TODO refactor
@SuppressWarnings("unused")
public final class NettyServerSocketHandler implements Runnable, AutoCloseable {
    private transient final LaunchServer server;
    public volatile boolean logConnections = Boolean.getBoolean("launcher.logConnections");

    public LauncherNettyServer nettyServer;
    private SSLServerSocketFactory ssf;

    public NettyServerSocketHandler(LaunchServer server) {
        this.server = server;
    }

    @Override
    public void close() {
        if (nettyServer == null) return;
        nettyServer.close();
        nettyServer.service.channels.close();
    }

    @Override
    public void run() {
        LogHelper.info("Starting netty server socket thread");
        nettyServer = new LauncherNettyServer(server);
        for (LaunchServerConfig.NettyBindAddress address : server.config.netty.binds) {
            nettyServer.bind(new InetSocketAddress(address.address, address.port));
        }
    }
}
