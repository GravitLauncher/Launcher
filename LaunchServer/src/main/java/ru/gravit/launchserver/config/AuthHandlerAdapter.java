package ru.gravit.launchserver.config;

import com.google.gson.*;
import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.socket.websocket.json.JsonResponseInterface;

import java.lang.reflect.Type;

public class AuthHandlerAdapter implements JsonSerializer<AuthHandler>, JsonDeserializer<AuthHandler> {
    private static final String PROP_NAME = "type";
    @Override
    public AuthHandler deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<AuthHandler> cls = AuthHandler.getHandlerClass(typename);


        return (AuthHandler) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(AuthHandler src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = AuthHandler.getHandlerName(src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
