package pro.gravit.launcher.request.websockets;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashSet;

import javax.net.ssl.SSLException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.events.ExceptionEvent;
import pro.gravit.launcher.events.request.*;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.launcher.hasher.HashedEntryAdapter;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.utils.ProviderMap;
import pro.gravit.utils.UniversalJsonAdapter;
import pro.gravit.utils.helper.LogHelper;

public class ClientWebSocketService extends ClientJSONPoint {
    public final Gson gson;
    public OnCloseCallback onCloseCallback;
    public final Boolean onConnect;
    public ReconnectCallback reconnectCallback;
    public static ProviderMap<WebSocketEvent> results = new ProviderMap<>();
    public static ProviderMap<WebSocketRequest> requests = new ProviderMap<>();
    private HashSet<EventHandler> handlers;

    public ClientWebSocketService(String address) throws SSLException {
        super(createURL(address));
        handlers = new HashSet<>();
        this.gson = Launcher.gsonManager.gson;
        this.onConnect = true;
    }

    public static void appendTypeAdapters(GsonBuilder builder)
    {
        builder.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        builder.registerTypeAdapter(WebSocketEvent.class, new UniversalJsonAdapter<>(ClientWebSocketService.results));
        builder.registerTypeAdapter(WebSocketRequest.class, new UniversalJsonAdapter<>(ClientWebSocketService.requests));
        builder.registerTypeAdapter(AuthRequest.AuthPasswordInterface.class, new UniversalJsonAdapter<>(AuthRequest.providers));
    }

    private static URI createURL(String address) {
        try {
            URI u = new URI(address);
            return u;
        } catch (Throwable e) {
            LogHelper.error(e);
            return null;
        }
    }

    @Override
    void onMessage(String message) {
        WebSocketEvent result = gson.fromJson(message, WebSocketEvent.class);
        for (EventHandler handler : handlers) {
            handler.process(result);
        }
    }

    @Override
    void onDisconnect() {
        LogHelper.info("WebSocket client disconnect");
        if (onCloseCallback != null) onCloseCallback.onClose(0, "unsupported param", !isClosed);
    }

    @Override
    void onOpen() throws Exception {
        synchronized (onConnect) {
            onConnect.notifyAll();
        }
    }

    @FunctionalInterface
    public interface OnCloseCallback {
        void onClose(int code, String reason, boolean remote);
    }

    public interface ReconnectCallback {
        void onReconnect() throws IOException;
    }

    public void registerRequests() {

    }

    public void registerResults() {
        results.register("auth", AuthRequestEvent.class);
        results.register("checkServer", CheckServerRequestEvent.class);
        results.register("joinServer", JoinServerRequestEvent.class);
        results.register("launcher", LauncherRequestEvent.class);
        results.register("profileByUsername", ProfileByUsernameRequestEvent.class);
        results.register("profileByUUID", ProfileByUUIDRequestEvent.class);
        results.register("batchProfileByUsername", BatchProfileByUsernameRequestEvent.class);
        results.register("profiles", ProfilesRequestEvent.class);
        results.register("setProfile", SetProfileRequestEvent.class);
        results.register("updateList", UpdateListRequestEvent.class);
        results.register("error", ErrorRequestEvent.class);
        results.register("update", UpdateRequestEvent.class);
        results.register("restoreSession", RestoreSessionRequestEvent.class);
        results.register("getSecureToken", GetSecureTokenRequestEvent.class);
        results.register("verifySecureToken", VerifySecureTokenRequestEvent.class);
        results.register("log", LogEvent.class);
        results.register("cmdExec", ExecCommandRequestEvent.class);
        results.register("getAvailabilityAuth", GetAvailabilityAuthRequestEvent.class);
        results.register("exception", ExceptionEvent.class);
        results.register("register", RegisterRequestEvent.class);
        results.register("setpassword", SetPasswordRequestEvent.class);
    }

    public void registerHandler(EventHandler eventHandler) {
        handlers.add(eventHandler);
    }

    public void waitIfNotConnected() {
        /*if(!isOpen() && !isClosed() && !isClosing())
        {
            LogHelper.warning("WebSocket not connected. Try wait onConnect object");
            synchronized (onConnect)
            {
                try {
                    onConnect.wait(5000);
                } catch (InterruptedException e) {
                    LogHelper.error(e);
                }
            }
        }*/
    }

    public void sendObject(Object obj) throws IOException {
        waitIfNotConnected();
        if (ch == null || !ch.isActive()) reconnectCallback.onReconnect();
        //if(isClosed() && reconnectCallback != null)
        //    reconnectCallback.onReconnect();
        send(gson.toJson(obj, WebSocketRequest.class));
    }

    public void sendObject(Object obj, Type type) throws IOException {
        waitIfNotConnected();
        if (ch == null || !ch.isActive()) reconnectCallback.onReconnect();
        //if(isClosed() && reconnectCallback != null)
        //    reconnectCallback.onReconnect();
        send(gson.toJson(obj, type));
    }

    @FunctionalInterface
    public interface EventHandler {
        void process(WebSocketEvent webSocketEvent);
    }
}
