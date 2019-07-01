package pro.gravit.launcher.request.websockets;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;

import javax.net.ssl.SSLException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pro.gravit.launcher.events.ExceptionEvent;
import pro.gravit.launcher.events.request.*;
import pro.gravit.launcher.hasher.HashedEntry;
import pro.gravit.launcher.hasher.HashedEntryAdapter;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.utils.helper.LogHelper;

public class ClientWebSocketService extends ClientJSONPoint {
    public final GsonBuilder gsonBuilder;
    public final Gson gson;
    public OnCloseCallback onCloseCallback;
    public final Boolean onConnect;
    public ReconnectCallback reconnectCallback;
    private HashMap<String, Class<? extends WebSocketRequest>> requests;
    private HashMap<String, Class<? extends WebSocketEvent>> results;
    private HashSet<EventHandler> handlers;

    public ClientWebSocketService(GsonBuilder gsonBuilder, String address, int i) throws SSLException {
        super(createURL(address));
        requests = new HashMap<>();
        results = new HashMap<>();
        handlers = new HashSet<>();
        this.gsonBuilder = gsonBuilder;
        this.gsonBuilder.registerTypeAdapter(WebSocketRequest.class, new JsonRequestAdapter(this));
        this.gsonBuilder.registerTypeAdapter(WebSocketEvent.class, new JsonResultAdapter(this));
        this.gsonBuilder.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        this.gson = gsonBuilder.create();
        this.onConnect = true;
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

    public Class<? extends WebSocketRequest> getRequestClass(String key) {
        return requests.get(key);
    }

    public Class<? extends WebSocketEvent> getResultClass(String key) {
        return results.get(key);
    }

    public void registerRequest(String key, Class<? extends WebSocketRequest> clazz) {
        requests.put(key, clazz);
    }

    public void registerRequests() {

    }

    public void registerResult(String key, Class<? extends WebSocketEvent> clazz) {
        results.put(key, clazz);
    }

    public void registerResults() {
        registerResult("auth", AuthRequestEvent.class);
        registerResult("checkServer", CheckServerRequestEvent.class);
        registerResult("joinServer", JoinServerRequestEvent.class);
        registerResult("launcher", LauncherRequestEvent.class);
        registerResult("profileByUsername", ProfileByUsernameRequestEvent.class);
        registerResult("profileByUUID", ProfileByUUIDRequestEvent.class);
        registerResult("batchProfileByUsername", BatchProfileByUsernameRequestEvent.class);
        registerResult("profiles", ProfilesRequestEvent.class);
        registerResult("setProfile", SetProfileRequestEvent.class);
        registerResult("updateList", UpdateListRequestEvent.class);
        registerResult("error", ErrorRequestEvent.class);
        registerResult("update", UpdateRequestEvent.class);
        registerResult("restoreSession", RestoreSessionRequestEvent.class);
        registerResult("getSecureToken", GetSecureTokenRequestEvent.class);
        registerResult("verifySecureToken", VerifySecureTokenRequestEvent.class);
        registerResult("log", LogEvent.class);
        registerResult("cmdExec", ExecCommandRequestEvent.class);
        registerResult("getAvailabilityAuth", GetAvailabilityAuthRequestEvent.class);
        registerResult("exception", ExceptionEvent.class);
        registerResult("register", RegisterRequestEvent.class);
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
