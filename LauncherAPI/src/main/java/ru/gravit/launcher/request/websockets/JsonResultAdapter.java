package ru.gravit.launcher.request.websockets;

import com.google.gson.*;
import ru.gravit.launcher.request.ResultInterface;

import java.lang.reflect.Type;

public class JsonResultAdapter implements JsonSerializer<ResultInterface>, JsonDeserializer<ResultInterface> {
    private final ClientWebSocketService service;
    private static final String PROP_NAME = "type";

    public JsonResultAdapter(ClientWebSocketService service) {
        this.service = service;
    }

    @Override
    public ResultInterface deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends ResultInterface> cls = service.getResultClass(typename);


        return (ResultInterface) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(ResultInterface src, Type typeOfSrc, JsonSerializationContext context) {
        // note : won't work, you must delegate this
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = src.getType();
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
