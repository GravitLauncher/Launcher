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

import ru.gravit.launchserver.auth.handler.AuthHandler;

public class AuthHandlerAdapter implements JsonSerializer<AuthHandler>, JsonDeserializer<AuthHandler> {
    private static final String PROP_NAME = "type";
    @Override
    public AuthHandler deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends AuthHandler> cls = AuthHandler.getHandlerClass(typename);


        return (AuthHandler) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(AuthHandler src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        @SuppressWarnings("unchecked")
		String classPath = AuthHandler.getHandlerName((Class<AuthHandler>) src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
