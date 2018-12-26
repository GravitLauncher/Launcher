package ru.gravit.launchserver.config;

import com.google.gson.*;
import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.auth.permissions.PermissionsHandler;

import java.lang.reflect.Type;

public class PermissionsHandlerAdapter implements JsonSerializer<PermissionsHandler>, JsonDeserializer<PermissionsHandler> {
    private static final String PROP_NAME = "type";
    @Override
    public PermissionsHandler deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<PermissionsHandler> cls = PermissionsHandler.getHandlerClass(typename);


        return (PermissionsHandler) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(PermissionsHandler src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = PermissionsHandler.getHandlerName(src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
