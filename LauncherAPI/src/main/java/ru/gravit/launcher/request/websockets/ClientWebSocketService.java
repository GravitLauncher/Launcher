package ru.gravit.launcher.request.websockets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.gravit.launcher.hasher.HashedEntry;
import ru.gravit.launcher.hasher.HashedEntryAdapter;
import ru.gravit.launcher.request.ResultInterface;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class ClientWebSocketService extends ClientJSONPoint {
    public final GsonBuilder gsonBuilder;
    public final Gson gson;
    private HashMap<String, Class<? extends RequestInterface>> requests;
    private HashMap<String, Class<? extends ResultInterface>> results;
    private HashSet<EventHandler> handlers;

    public ClientWebSocketService(GsonBuilder gsonBuilder, String address, int port, int i) {
    	super(createURL(address, port), Collections.emptyMap(), i);
        requests = new HashMap<>();
        results = new HashMap<>();
        handlers = new HashSet<>();
        this.gsonBuilder = gsonBuilder;
        gsonBuilder.registerTypeAdapter(RequestInterface.class, new JsonRequestAdapter(this));
        gsonBuilder.registerTypeAdapter(HashedEntry.class, new HashedEntryAdapter());
        this.gson = gsonBuilder.create();
    }
	private static URI createURL(String address, int port) {
		try {
			URL u = new URL(address);
			return new URL(u.getProtocol(), u.getHost(), port, u.getFile()).toURI();
		} catch (Throwable e) {
			LogHelper.error(e);
			return null;
		}
	}
	@Override
	public void onMessage(String message) {
        ResultInterface result = gson.fromJson(message, ResultInterface.class);
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

    public void sendObject(Object obj) throws IOException {
        send(gson.toJson(obj));
    }
    @FunctionalInterface
    public interface EventHandler
    {
        void process(ResultInterface resultInterface);
    }
}
