package ru.gravit.launchserver.config;

import com.google.gson.*;
import ru.gravit.launchserver.texture.TextureProvider;
import ru.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;

public class TextureProviderAdapter implements JsonSerializer<TextureProvider>, JsonDeserializer<TextureProvider> {
    private static final String PROP_NAME = "type";

    @Override
    public TextureProvider deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends TextureProvider> cls = TextureProvider.getProviderClass(typename);
        if(cls == null)
        {
            LogHelper.error("TextureProvider %s not found", typename);
            return null;
        }


        return (TextureProvider) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(TextureProvider src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = TextureProvider.getProviderName(src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
