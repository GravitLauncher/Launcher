package ru.gravit.launchserver.socket.websocket.json;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;

public class JsonResponseAdapter implements JsonSerializer<JsonResponseInterface>, JsonDeserializer<JsonResponseInterface> {
    static HashMap<String,Class> map = new HashMap<>();
    private static final String PROP_NAME = "type";
    static {
        map.put("echo",EchoResponse.class);
    }
    @Override
    public JsonResponseInterface deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class cls = map.get(typename);


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
