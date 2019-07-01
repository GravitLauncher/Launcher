package pro.gravit.launcher.request.websockets;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import pro.gravit.utils.helper.LogHelper;

public class JsonRequestAdapter implements JsonSerializer<WebSocketRequest>, JsonDeserializer<WebSocketRequest> {
    private final ClientWebSocketService service;
    private static final String PROP_NAME = "type";

    public JsonRequestAdapter(ClientWebSocketService service) {
        this.service = service;
    }

    @Override
    public WebSocketRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends WebSocketRequest> cls = service.getRequestClass(typename);
        if (cls == null) {
            LogHelper.error("Request type %s not found", typename);
        }


        return (WebSocketRequest) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(WebSocketRequest src, Type typeOfSrc, JsonSerializationContext context) {
        // note : won't work, you must delegate this
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = src.getType();
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
