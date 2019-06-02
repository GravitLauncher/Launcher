package pro.gravit.launcher.request.websockets;

import com.google.gson.*;
import pro.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;

public class JsonRequestAdapter implements JsonSerializer<RequestInterface>, JsonDeserializer<RequestInterface> {
    private final ClientWebSocketService service;
    private static final String PROP_NAME = "type";

    public JsonRequestAdapter(ClientWebSocketService service) {
        this.service = service;
    }

    @Override
    public RequestInterface deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends RequestInterface> cls = service.getRequestClass(typename);
        if (cls == null) {
            LogHelper.error("Request type %s not found", typename);
        }


        return (RequestInterface) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(RequestInterface src, Type typeOfSrc, JsonSerializationContext context) {
        // note : won't work, you must delegate this
        JsonObject jo = context.serialize(src).getAsJsonObject();

        String classPath = src.getType();
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
