package ru.gravit.launchserver.socket.websocket.json;

import com.google.gson.*;
import ru.gravit.launchserver.socket.websocket.WebSocketService;

import java.lang.reflect.Type;

public class JsonResponseAdapter implements JsonSerializer<JsonResponseInterface>, JsonDeserializer<JsonResponseInterface> {
    private final WebSocketService service;
    private static final String PROP_NAME = "type";

    public JsonResponseAdapter(WebSocketService service) {
        this.service = service;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonResponseInterface deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<JsonResponseInterface> cls = service.getResponseClass(typename);


        return (JsonResponseInterface) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(JsonResponseInterface src, Type typeOfSrc, JsonSerializationContext context) {
        // note : won't work, you must delegate this
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = src.getType();
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
