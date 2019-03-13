package ru.gravit.launchserver.config;

import com.google.gson.*;
import ru.gravit.launchserver.components.Component;
import ru.gravit.utils.helper.LogHelper;

import java.lang.reflect.Type;

public class ComponentAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {
    private static final String PROP_NAME = "component";

    @Override
    public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<? extends Component> cls = Component.getComponentClass(typename);
        if(cls == null)
        {
            LogHelper.error("Component %s not found", typename);
            return null;
        }


        return (Component) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jo = context.serialize(src).getAsJsonObject();

        @SuppressWarnings("unchecked")
        String classPath = Component.getComponentName((Class<Component>) src.getClass());
        jo.add(PROP_NAME, new JsonPrimitive(classPath));

        return jo;
    }
}
