package pro.gravit.launchserver.socket.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.launchserver.socket.LauncherNettyServer;

import javax.net.ssl.SSLServerSocketFactory;
import java.net.InetSocketAddress;

@SuppressWarnings("unused")
public final class NettyServerSocketHandler implements Runnable, AutoCloseable {
    private transient final LaunchServer server;
    private transient final Logger logger = LogManager.getLogger();
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
        logger.info("Starting netty server socket thread");
        nettyServer = new LauncherNettyServer(server);
        for (LaunchServerConfig.NettyBindAddress address : server.config.netty.binds) {
            nettyServer.bind(new InetSocketAddress(address.address, address.port));
        }
    }
}
