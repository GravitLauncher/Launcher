package ru.gravit.launcher.request;

import com.google.gson.*;
import ru.gravit.launcher.request.ResultInterface;

import java.lang.reflect.Type;

public class JsonResultSerializeAdapter implements JsonSerializer<ResultInterface> {
    private static final String PROP_NAME = "type";

    @Override
    public JsonElement serialize(ResultInterface src, Type typeOfSrc, JsonSerializationContext context) {
        // note : won't work, you must delegate this
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = src.getType();
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
