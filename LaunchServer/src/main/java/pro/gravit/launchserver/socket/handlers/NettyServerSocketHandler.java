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
        /*SSLContext sc = null;
        try {
            sc = SSLContextInit();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }*/
        //System.setProperty( "javax.net.ssl.keyStore","keystore");
        //System.setProperty( "javax.net.ssl.keyStorePassword","PSP1000");
        /*try {
            Selector selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        LogHelper.info("Starting netty server socket thread");
        //SSLEngine engine = sc.createSSLEngine();
        //engine.setUseClientMode(false);
        nettyServer = new LauncherNettyServer(server);
        for (LaunchServerConfig.NettyBindAddress address : server.config.netty.binds) {
            nettyServer.bind(new InetSocketAddress(address.address, address.port));
        }
        /*
        try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket()) {
            serverSocket.setEnabledProtocols(new String[] {"TLSv1.2"});
            if (!this.serverSocket.compareAndSet(null, serverSocket)) {
                throw new IllegalStateException("Previous socket wasn't closed");
            }

            // Set socket params
            serverSocket.setReuseAddress(true);
            serverSocket.setPerformancePreferences(1, 0, 2);
            //serverSocket.setReceiveBufferSize(0x10000);
            serverSocket.bind(server.config.getSocketAddress());
            LogHelper.info("Server socket thread successfully started");
            // Listen for incoming connections
            while (serverSocket.isBound()) {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                sockets.add(socket);
                socket.startHandshake();
                // Invoke pre-connect listener
                long id = idCounter.incrementAndGet();
                if (listener != null && !listener.onConnect(id, socket.getInetAddress())) {
                    continue; // Listener didn't accepted this connection
                }

                // Reply in separate thread
                threadPool.execute(new ResponseThread(server, id, socket));
            }
        } catch (IOException e) {
            // Ignore error after close/rebind
            if (serverSocket.get() != null) {
                LogHelper.error(e);
            }
        }
        */
    }
}
