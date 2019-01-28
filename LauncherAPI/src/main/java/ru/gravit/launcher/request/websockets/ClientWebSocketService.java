package ru.gravit.launcher.request.websockets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedEntryAdapter;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;

public class ClientWebSocketService extends ClientJSONPoint {
    public final GsonBuilder gsonBuilder;
    public final Gson gson;
    private HashMap<String, Class<? extends RequestInterface>> requests;
    private HashMap<String, Class<? extends ResultInterface>> results;
    private HashSet<EventHandler> handlers;

    public ClientWebSocketService(GsonBuilder gsonBuilder) {
        requests = new HashMap<>();
        results = new HashMap<>();
        handlers = new HashSet<>();
        this.gsonBuilder = gsonBuilder;
        gsonBuilder.registerTypeAdapter(RequestInterface.class, new JsonRequestAdapter(this));
        gsonBuilder.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        this.gson = gsonBuilder.create();
    }
    @Override
    public void processMessage(Reader reader) {
        ResultInterface result = gson.fromJson(reader, ResultInterface.class);
        for(EventHandler handler : handlers)
        {
            handler.process(result);
        }
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

    }

    public void registerHandler(EventHandler eventHandler)
    {
        handlers.add(eventHandler);
    }

    public void sendObjectAsync(Object obj) throws IOException {
        sendAsync(gson.toJson(obj));
    }

    public void sendObject(Object obj) throws IOException {
        send(gson.toJson(obj));
    }
    @FunctionalInterface
    public interface EventHandler
    {
        void process(ResultInterface resultInterface);
    }
}
