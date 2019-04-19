package ru.gravit.utils;

import com.google.gson.*;
import ru.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;

public class UniversalJsonAdapter<R> implements JsonSerializer<R>, JsonDeserializer<R> {
    public ProviderMap<R> providerMap;
    public String PROP_NAME = "type";

    public UniversalJsonAdapter(ProviderMap<R> providerMap) {
        this.providerMap = providerMap;
    }

    public UniversalJsonAdapter(ProviderMap<R> providerMap, String PROP_NAME) {
        this.providerMap = providerMap;
        this.PROP_NAME = PROP_NAME;
    }

    public R deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends R> cls = providerMap.getProviderClass(typename);
        if (cls == null) {
            LogHelper.error("AuthHandler %s not found", typename);
            return null;
        }
        return context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(R src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        @SuppressWarnings("unchecked")
        String classPath = providerMap.getProviderName((Class<? extends R>) src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
