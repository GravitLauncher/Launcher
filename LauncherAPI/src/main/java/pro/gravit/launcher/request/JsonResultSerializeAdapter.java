package pro.gravit.launcher.request;

import com.google.gson.*;

import java.lang.reflect.Type;

public class JsonResultSerializeAdapter implements JsonSerializer<WebSocketEvent> {
    private static final String PROP_NAME = "type";

    @Override
    public JsonElement serialize(WebSocketEvent src, Type typeOfSrc, JsonSerializationContext context) {
        // note : won't work, you must delegate this
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = src.getType();
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
