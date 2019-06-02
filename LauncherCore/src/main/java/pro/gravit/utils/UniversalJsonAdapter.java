package pro.gravit.utils;

import com.google.gson.*;
import pro.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;

public class UniversalJsonAdapter<R> implements JsonSerializer<R>, JsonDeserializer<R> {
    public final ProviderMap<R> providerMap;
    public final String name;
    public final String PROP_NAME;

    public UniversalJsonAdapter(ProviderMap<R> providerMap) {
        this.providerMap = providerMap;
        this.name = providerMap.getName();
        this.PROP_NAME = "type";
    }

    public UniversalJsonAdapter(ProviderMap<R> providerMap, String PROP_NAME) {
        this.providerMap = providerMap;
        this.name = providerMap.getName();
        this.PROP_NAME = PROP_NAME;
    }

    public R deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends R> cls = providerMap.getClass(typename);
        if (cls == null) {
            LogHelper.error("%s %s not found", name, typename);
            return null;
        }
        return context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(R src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        @SuppressWarnings("unchecked")
        String classPath = providerMap.getName((Class<? extends R>) src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
