package ru.gravit.launchserver.config;

import com.google.gson.*;
import ru.gravit.launchserver.auth.hwid.HWIDHandler;

import java.lang.reflect.Type;

public class HWIDHandlerAdapter implements JsonSerializer<HWIDHandler>, JsonDeserializer<HWIDHandler> {
    private static final String PROP_NAME = "type";

    @Override
    public HWIDHandler deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends HWIDHandler> cls = HWIDHandler.getHandlerClass(typename);


        return (HWIDHandler) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(HWIDHandler src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = HWIDHandler.getHandlerName(src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
