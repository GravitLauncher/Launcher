package ru.gravit.launcher.hasher;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class HashedEntryAdapter implements JsonSerializer<HashedEntry>, JsonDeserializer<HashedEntry> {
    private static final String PROP_NAME = "type";

    public HashedEntryAdapter() {

    }

    @Override
    public HashedEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String typename = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
        Class<?> cls = null;
        if (typename.equals("dir")) cls = HashedDir.class;
        if (typename.equals("file")) cls = HashedFile.class;


        return (HashedEntry) context.deserialize(json, cls);
    }

    @Override
    public JsonElement serialize(HashedEntry src, Type typeOfSrc, JsonSerializationContext context) {
        // note : won't work, you must delegate this
        JsonObject jo = context.serialize(src).getAsJsonObject();

        HashedEntry.Type type = src.getType();
        if (type == HashedEntry.Type.DIR)
            jo.add(PROP_NAME, new JsonPrimitive("dir"));
        if (type == HashedEntry.Type.FILE)
            jo.add(PROP_NAME, new JsonPrimitive("file"));

        return jo;
    }
}
