package ru.gravit.launchserver.socket.websocket.json;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;

public class JsonResponseAdapter implements JsonSerializer<JsonResponse>, JsonDeserializer<JsonResponse> {
    static HashMap<String,Class<JsonResponse>> map = new HashMap<>();
    private static final String PROP_NAME = "type";
    @Override
    public JsonResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<JsonResponse> cls = map.get(typename);


        return (JsonResponse) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(JsonResponse src, Type typeOfSrc, JsonSerializationContext context) {
        // note : won't work, you must delegate this
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = src.getType();
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
