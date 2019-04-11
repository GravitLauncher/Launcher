package ru.gravit.launcher.request.websockets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.events.request.*;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedEntryAdapter;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class ClientWebSocketService extends ClientJSONPoint {
    public final GsonBuilder gsonBuilder;
    public final Gson gson;
    public OnCloseCallback onCloseCallback;
    public ReconnectCallback reconnectCallback;
    private HashMap<String, Class<? extends RequestInterface>> requests;
    private HashMap<String, Class<? extends ResultInterface>> results;
    private HashSet<EventHandler> handlers;

    public ClientWebSocketService(GsonBuilder gsonBuilder, String address, int i) {
        super(createURL(address), Collections.emptyMap(), i);
        requests = new HashMap<>();
        results = new HashMap<>();
        handlers = new HashSet<>();
        this.gsonBuilder = gsonBuilder;
        this.gsonBuilder.registerTypeAdapter(RequestInterface.class, new JsonRequestAdapter(this));
        this.gsonBuilder.registerTypeAdapter(ResultInterface.class, new JsonResultAdapter(this));
        this.gsonBuilder.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        this.gson = gsonBuilder.create();
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
    public void onMessage(String message) {
        ResultInterface result = gson.fromJson(message, ResultInterface.class);
        for (EventHandler handler : handlers) {
            handler.process(result);
        }
    }

    @Override
    public void onError(Exception e) {
        LogHelper.error(e);
    }

    @Override
    public void onClose(int code, String reason, boolean remote)
    {
        LogHelper.debug("Disconnected: " + code + " " + remote + " " + (reason != null ? reason : "no reason"));
        if(onCloseCallback != null)
            onCloseCallback.onClose(code, reason, remote);
    }
    @FunctionalInterface
    public interface OnCloseCallback
    {
        void onClose(int code, String reason, boolean remote);
    }
    public interface ReconnectCallback
    {
        void onReconnect() throws IOException;
    }

    public Class<? extends RequestInterface> getRequestClass(String key) {
        return requests.get(key);
    }

    public Class<? extends ResultInterface> getResultClass(String key) {
        return results.get(key);
    }

    public void registerRequest(String key, Class<? extends RequestInterface> clazz) {
        requests.put(key, clazz);
    }

    public void registerRequests() {

    }

    public void registerResult(String key, Class<? extends ResultInterface> clazz) {
        results.put(key, clazz);
    }

    public void registerResults() {
        registerResult("echo", EchoRequestEvent.class);
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
    }

    public void registerHandler(EventHandler eventHandler) {
        handlers.add(eventHandler);
    }

    public void sendObject(Object obj) throws IOException {
        if(isClosed() && reconnectCallback != null)
            reconnectCallback.onReconnect();
        send(gson.toJson(obj, RequestInterface.class));
    }

    public void sendObject(Object obj, Type type) throws IOException {
        if(isClosed() && reconnectCallback != null)
            reconnectCallback.onReconnect();
        send(gson.toJson(obj, type));
    }

    @FunctionalInterface
    public interface EventHandler {
        void process(ResultInterface resultInterface);
    }
}
