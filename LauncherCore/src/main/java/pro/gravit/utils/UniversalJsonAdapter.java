package pro.gravit.utils;

import com.google.gson.*;
import pro.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;

/**
 * An adapter that uses {@link ProviderMap} to serialize and deserialize a group of similar objects
 *
 * @param <R> Class or interface type
 * @see ProviderMap
 */
public class UniversalJsonAdapter<R> implements JsonSerializer<R>, JsonDeserializer<R> {
    public final ProviderMap<R> providerMap;
    public final String name;
    public final String PROP_NAME;
    public final boolean printErrorIfUnknownType;

    public UniversalJsonAdapter(ProviderMap<R> providerMap) {
        this.providerMap = providerMap;
        this.name = providerMap.getName();
        this.PROP_NAME = "type";
        printErrorIfUnknownType = true;
    }

    public UniversalJsonAdapter(ProviderMap<R> providerMap, String PROP_NAME) {
        this.providerMap = providerMap;
        this.name = providerMap.getName();
        this.PROP_NAME = PROP_NAME;
        printErrorIfUnknownType = true;
    }

    public UniversalJsonAdapter(ProviderMap<R> providerMap, String name, String PROP_NAME, boolean printErrorIfUnknownType) {
        this.providerMap = providerMap;
        this.name = name;
        this.PROP_NAME = PROP_NAME;
        this.printErrorIfUnknownType = printErrorIfUnknownType;
    }

    public UniversalJsonAdapter(ProviderMap<R> providerMap, String name, boolean printErrorIfUnknownType) {
        this.providerMap = providerMap;
        this.name = name;
        this.PROP_NAME = "type";
        this.printErrorIfUnknownType = printErrorIfUnknownType;
    }

    public R deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends R> cls = providerMap.getClass(typename);
        if (cls == null) {
            if (printErrorIfUnknownType) LogHelper.error("%s %s not found", name, typename);
            return null;
        }
        return context.deserialize(json, cls);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonElement serialize(R src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();
        String classPath = providerMap.getName((Class<? extends R>) src.getClass());
        if (classPath == null && src instanceof TypeSerializeInterface) {
            classPath = ((TypeSerializeInterface) src).getType();
        }
        if (classPath == null) {
            if (printErrorIfUnknownType) LogHelper.warning("Class %s type null", src.getClass());
        } else jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
