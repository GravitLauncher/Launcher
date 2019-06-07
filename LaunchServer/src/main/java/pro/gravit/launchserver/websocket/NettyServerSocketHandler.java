package pro.gravit.launchserver.websocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;

import pro.gravit.launcher.ssl.LauncherKeyStore;
import pro.gravit.launcher.ssl.LauncherTrustManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.legacy.Response;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.VerifyHelper;

@SuppressWarnings({"unused", "rawtypes"})
public final class NettyServerSocketHandler implements Runnable, AutoCloseable {
    private SSLServerSocketFactory ssf;

    public volatile boolean logConnections = Boolean.getBoolean("launcher.logConnections");

    public LauncherNettyServer nettyServer;

    private final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();

    // API
    private final Map<String, Response.Factory> customResponses = new ConcurrentHashMap<>(2);
    private final AtomicLong idCounter = new AtomicLong(0L);
    private Set<Socket> sockets;
    private volatile Listener listener;

	private transient final LaunchServer server;

    public NettyServerSocketHandler(LaunchServer server) {
        this.server = server;
    }

    @Override
    public void close() {
        ServerSocket socket = serverSocket.getAndSet(null);
        if (socket != null) {
            LogHelper.info("Closing server socket listener");
            try {
                socket.close();
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
    }

    public SSLContext SSLContextInit() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException, IOException, CertificateException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new LauncherTrustManager()
        };
        KeyStore ks = LauncherKeyStore.getKeyStore("keystore", "PSP1000");

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                .getDefaultAlgorithm());
        kmf.init(ks, "PSP1000".toCharArray());
        SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
        return sc;
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
        for (LaunchServer.NettyBindAddress address : server.config.netty.binds) {
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


    public void registerCustomResponse(String name, Response.Factory factory) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(customResponses, name, Objects.requireNonNull(factory, "factory"),
                String.format("Custom response has been already registered: '%s'", name));
    }


    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /*package*/ void onDisconnect(long id, Exception e) {
        if (listener != null) {
            listener.onDisconnect(id, e);
        }
    }

    /*package*/ boolean onHandshake(long id, Integer type) {
        return listener == null || listener.onHandshake(id, type);
    }

    public interface Listener {

        boolean onConnect(long id, InetAddress address);


        void onDisconnect(long id, Exception e);


        boolean onHandshake(long id, Integer type);
    }
}
