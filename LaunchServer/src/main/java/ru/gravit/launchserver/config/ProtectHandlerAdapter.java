package ru.gravit.launchserver.config;

import com.google.gson.*;
import ru.gravit.launchserver.auth.protect.ProtectHandler;
import ru.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;

public class ProtectHandlerAdapter implements JsonSerializer<ProtectHandler>, JsonDeserializer<ProtectHandler> {
    private static final String PROP_NAME = "type";

    @Override
    public ProtectHandler deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends ProtectHandler> cls = ProtectHandler.getHandlerClass(typename);
        if(cls == null)
        {
            LogHelper.error("ProtectHandler %s not found", typename);
            return null;
        }


        return (ProtectHandler) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(ProtectHandler src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        @SuppressWarnings("unchecked")
        String classPath = ProtectHandler.getHandlerName((Class<ProtectHandler>) src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
