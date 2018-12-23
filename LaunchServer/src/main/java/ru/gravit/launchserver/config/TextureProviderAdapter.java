package ru.gravit.launchserver.config;

import com.google.gson.*;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.texture.TextureProvider;

import java.lang.reflect.Type;

public class TextureProviderAdapter implements JsonSerializer<TextureProvider>, JsonDeserializer<TextureProvider> {
    private static final String PROP_NAME = "type";
    @Override
    public TextureProvider deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<AuthProvider> cls = TextureProvider.getProviderClass(typename);


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
