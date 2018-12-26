package ru.gravit.launchserver.config;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import ru.gravit.launchserver.auth.provider.AuthProvider;

public class AuthProviderAdapter implements JsonSerializer<AuthProvider>, JsonDeserializer<AuthProvider> {
    private static final String PROP_NAME = "type";
    @Override
    public AuthProvider deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends AuthProvider> cls = AuthProvider.getProviderClass(typename);


        return (AuthProvider) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(AuthProvider src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = AuthProvider.getProviderName(src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
