package ru.gravit.launcher.request.websockets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedEntryAdapter;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

public class ClientWebSocketService {
    public final GsonBuilder gsonBuilder;
    public final Gson gson;
    public final ClientJSONPoint point;
    private HashMap<String, Class<RequestInterface>> requests;
    private HashMap<String, Class<ResultInterface>> results;

    public ClientWebSocketService(GsonBuilder gsonBuilder, ClientJSONPoint point) {
        requests = new HashMap<>();
        results = new HashMap<>();
        this.gsonBuilder = gsonBuilder;
        gsonBuilder.registerTypeAdapter(RequestInterface.class, new JsonRequestAdapter(this));
        gsonBuilder.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        this.gson = gsonBuilder.create();
        this.point = point;
        point.setService(this);
    }

    public void processMessage(Reader reader) {
        ResultInterface result = gson.fromJson(reader, ResultInterface.class);
        result.process();
    }

    public Class<RequestInterface> getRequestClass(String key) {
        return requests.get(key);
    }

    public void registerRequest(String key, Class<RequestInterface> clazz) {
        requests.put(key, clazz);
    }

    public void registerRequests() {

    }

    public void registerResult(String key, Class<ResultInterface> clazz) {
        results.put(key, clazz);
    }

    public void registerResults() {

    }

    public void sendObjectAsync(Object obj) throws IOException {
        point.sendAsync(gson.toJson(obj));
    }

    public void sendObject(Object obj) throws IOException {
        point.send(gson.toJson(obj));
    }
}
