package pro.gravit.utils.helper;

import com.google.gson.*;
import pro.gravit.utils.command.CommandException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommonHelper {

    private static ScriptEngineFactory nashornFactory;

    static {
        try {
            ScriptEngineManager scriptManager = new ScriptEngineManager();
            nashornFactory = getEngineFactories(scriptManager);
        } catch (Throwable e) {
            nashornFactory = null;
        }
    }

    private CommonHelper() {
    }

    private static ScriptEngineFactory getEngineFactories(ScriptEngineManager manager) {
        // Метод похож на костыль но таковым не является, ибо единоразовое получение фактории быстрее, чем её переполучение на ходу.
        for (ScriptEngineFactory fact : manager.getEngineFactories())
            if (fact.getNames().contains("nashorn") || fact.getNames().contains("Nashorn")) return fact;
        return null;
    }

    public static String low(String s) {
        return s.toLowerCase(Locale.US);
    }

    public static boolean multiMatches(Pattern[] pattern, String from) {
        for (Pattern p : pattern)
            if (p.matcher(from).matches()) return true;
        return false;
    }

    public static String multiReplace(Pattern[] pattern, String from, String replace) {
        Matcher m;
        String tmp = null;
        for (Pattern p : pattern) {
            m = p.matcher(from);
            if (m.matches()) tmp = m.replaceAll(replace);
        }
        return tmp != null ? tmp : from;
    }

    public static ScriptEngine newScriptEngine() {
        if (nashornFactory == null) {
            throw new UnsupportedOperationException("ScriptEngine not supported");
        }
        return nashornFactory.getScriptEngine();
    }

    public static Thread newThread(String name, boolean daemon, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(daemon);
        if (name != null)
            thread.setName(name);
        return thread;
    }

    public static String replace(String source, String... params) {
        for (int i = 0; i < params.length; i += 2)
            source = source.replace('%' + params[i] + '%', params[i + 1]);
        return source;
    }

    public static String[] parseCommand(CharSequence line) throws CommandException {
        boolean quoted = false;
        boolean wasQuoted = false;

        // Read line char by char
        Collection<String> result = new LinkedList<>();
        StringBuilder builder = new StringBuilder(100);
        for (int i = 0; i <= line.length(); i++) {
            boolean end = i >= line.length();
            char ch = end ? '\0' : line.charAt(i);

            // Maybe we should read next argument?
            if (end || !quoted && Character.isWhitespace(ch)) {
                if (end && quoted)
                    throw new CommandException("Quotes wasn't closed");

                // Empty args are ignored (except if was quoted)
                if (wasQuoted || builder.length() > 0)
                    result.add(builder.toString());

                // Reset file builder
                wasQuoted = false;
                builder.setLength(0);
                continue;
            }

            // Append next char
            switch (ch) {
                case '"': // "abc"de, "abc""de" also allowed
                    quoted = !quoted;
                    wasQuoted = true;
                    break;
                case '\\': // All escapes, including spaces etc
                    if (i + 1 >= line.length())
                        throw new CommandException("Escape character is not specified");
                    char next = line.charAt(i + 1);
                    builder.append(next);
                    i++;
                    break;
                default: // Default char, simply append
                    builder.append(ch);
                    break;
            }
        }

        // Return result as array
        return result.toArray(new String[0]);
    }


    public static GsonBuilder newBuilder() {
        return new GsonBuilder().registerTypeHierarchyAdapter(byte[].class,
                ByteArrayToBase64TypeAdapter.INSTANCE);
    }

    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        private static final ByteArrayToBase64TypeAdapter INSTANCE = new ByteArrayToBase64TypeAdapter();
        private final Base64.Decoder decoder = Base64.getUrlDecoder();
        private final Base64.Encoder encoder = Base64.getUrlEncoder();

        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonArray()) {
                JsonArray byteArr = json.getAsJsonArray();
                byte[] arr = new byte[byteArr.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = byteArr.get(i).getAsByte();
                }
                return arr;
            }
            return decoder.decode(json.getAsString());
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(encoder.encodeToString(src));
        }
    }
}
