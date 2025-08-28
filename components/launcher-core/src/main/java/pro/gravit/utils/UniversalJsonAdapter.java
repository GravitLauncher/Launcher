package pro.gravit.utils;

import com.google.gson.*;

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
    public Class<? extends R> defaultClass;

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

    public UniversalJsonAdapter(ProviderMap<R> providerMap, String name, Class<? extends R> defaultClass) {
        this.providerMap = providerMap;
        this.name = name;
        this.defaultClass = defaultClass;
        this.PROP_NAME = "type";
    }

    public UniversalJsonAdapter(ProviderMap<R> providerMap, Class<? extends R> defaultClass) {
        this.providerMap = providerMap;
        this.defaultClass = defaultClass;
        this.name = providerMap.getName();
        this.PROP_NAME = "type";
    }

    public UniversalJsonAdapter(ProviderMap<R> providerMap, String name, String PROP_NAME, Class<? extends R> defaultClass) {
        this.providerMap = providerMap;
        this.name = name;
        this.PROP_NAME = PROP_NAME;
        this.defaultClass = defaultClass;
    }

    public R deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        if(typename == null) {
            throw new JsonParseException(String.format("%s: missing type property", name));
        }
        Class<? extends R> cls = providerMap.getClass(typename);
        if (cls == null) {
            if (defaultClass != null) {
                return context.deserialize(json, defaultClass);
            } else {
                throw new JsonParseException(String.format("%s: type %s not registered", name, typename));
            }
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
        if (classPath != null) {
            jo.add(PROP_NAME, new JsonPrimitive(classPath));
        } else {
            throw new JsonParseException(String.format("Class %s not registered", src.getClass().getName()));
        }
        return jo;
    }
}
