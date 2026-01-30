package pro.gravit.launcher.base.request.websockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.core.LauncherInject;
import pro.gravit.utils.helper.LogHelper;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class ClientJSONPoint implements WebSocket.Listener {

    private static final Logger logger =
            LoggerFactory.getLogger(ClientJSONPoint.class);

    @LauncherInject("launcher.certificatePinning")
    private static boolean isCertificatePinning;
    private static final AtomicInteger counter = new AtomicInteger();
    private final URI uri;
    public boolean isClosed;
    private final WebSocket.Builder webSocketBuilder;
    protected HttpClient httpClient;
    protected WebSocket webSocket;
    protected boolean ssl = false;
    protected int port;
    private final Object syncObject = new Object();
    private final Object sendSyncObject = new Object();
    private volatile StringBuilder builder = new StringBuilder();

    public ClientJSONPoint(final String uri) {
        this(URI.create(uri));
    }

    public ClientJSONPoint(URI uri) {
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
        try {
            var httpClientBuilder = HttpClient.newBuilder();
            if(isCertificatePinning) {
                httpClientBuilder = httpClientBuilder.sslContext(Downloader.makeSSLContext());
            }
            httpClient = httpClientBuilder.build();
            webSocketBuilder = httpClient.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(30));
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException |
                 KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public void connect() throws Exception {
        webSocket = webSocketBuilder.buildAsync(uri, this).get();
    }

    public void openAsync(Runnable onConnect, Consumer<Throwable> onFail) {
        webSocketBuilder.buildAsync(uri, this).thenAccept((e) -> {
            this.webSocket = e;
            onConnect.run();
        }).exceptionally((ex) -> {
            onFail.accept(ex);
            return null;
        });
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        synchronized (syncObject) {
            builder.append(data);
            if(last) {
                String message = builder.toString();
                builder = new StringBuilder();
                logger.trace("Received {}", message);
                onMessage(message);
            }
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        onDisconnect(statusCode, reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        onOpen();
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        return WebSocket.Listener.super.onBinary(webSocket, data, last);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        logger.error("", error);
        WebSocket.Listener.super.onError(webSocket, error);
    }

    public void send(String text) {
        logger.trace("Send {}", text);
        webSocket.sendText(text, true);
    }

    abstract void onMessage(String message);

    abstract void onDisconnect(int statusCode, String reason);

    abstract void onOpen();

    public void close() {
        webSocket.abort();
    }

}