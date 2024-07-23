package pro.gravit.utils.helper;

import com.google.gson.*;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.launch.LaunchOptions;

import javax.script.ScriptEngine;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommonHelper {

    private CommonHelper() {
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

    @Deprecated
    public static ScriptEngine newScriptEngine() {
        throw new UnsupportedOperationException("ScriptEngine not supported");
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

    public static String replace(Map<String, String> replaceMap, String arg) {
        for(var e : replaceMap.entrySet()) {
            arg = arg.replace(e.getKey(), e.getValue());
        }
        return arg;
    }

    public static List<String> replace(Map<String, String> replaceMap, List<String> args) {
        List<String> updatedList = new ArrayList<>(args.size());
        for(var e : args) {
            updatedList.add(replace(replaceMap, e));
        }
        return updatedList;
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
                if (wasQuoted || !builder.isEmpty())
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


    public static ArgsParseResult parseJavaArgs(List<String> args) {
        List<String> classpath = new ArrayList<>();
        List<String> jvmArgs = new ArrayList<>();
        List<String> runArgs = new ArrayList<>();
        String jarFile = null;
        String mainClass = null;
        String mainModule = null;
        LaunchOptions.ModuleConf conf = new LaunchOptions.ModuleConf();
        var prevArgType = PrevArgType.NONE;
        boolean runArgsBoolean = false;
        boolean first = false;
        for(var arg : args) {
            if(runArgsBoolean) {
                runArgs.add(arg);
                continue;
            }
            if(!first) {
                if(!arg.startsWith("-")) {
                    continue;
                }
                first = true;
            }
            switch (prevArgType) {
                case NONE -> {

                }
                case MODULE_PATH -> {
                    char c = ':';
                    int i = arg.indexOf(c);
                    if(i<0) {
                        c = ';';
                    }
                    String[] l = arg.split(Character.toString(c));
                    conf.modulePath.addAll(Arrays.asList(l));
                    prevArgType = PrevArgType.NONE;
                    continue;
                }
                case CLASSPATH -> {
                    char c = ':';
                    int i = arg.indexOf(c);
                    if(i<0) {
                        c = ';';
                    }
                    String[] l = arg.split(Character.toString(c));
                    classpath.addAll(Arrays.asList(l));
                    prevArgType = PrevArgType.POST_CLASSPATH;
                    continue;
                }
                case ADD_MODULES -> {
                    String[] l = arg.split(",");
                    conf.modules.addAll(Arrays.asList(l));
                    prevArgType = PrevArgType.NONE;
                    continue;
                }
                case ADD_OPENS -> {
                    String[] l = arg.split("=");
                    conf.opens.put(l[0], l[1]);
                    prevArgType = PrevArgType.NONE;
                    continue;
                }
                case ADD_EXPORTS -> {
                    String[] l = arg.split("=");
                    conf.exports.put(l[0], l[1]);
                    prevArgType = PrevArgType.NONE;
                    continue;
                }
                case ADD_READS -> {
                    String[] l = arg.split("=");
                    if(l.length != 2) {
                        continue;
                    }
                    conf.reads.put(l[0], l[1]);
                    prevArgType = PrevArgType.NONE;
                    continue;
                }
                case MODULE -> {
                    String[] l = arg.split("/");
                    mainModule = l[0];
                    mainClass = l[1];
                    runArgsBoolean = true;
                    prevArgType = PrevArgType.NONE;
                    continue;
                }
                case POST_CLASSPATH -> {
                    mainClass = arg;
                    runArgsBoolean = true;
                    prevArgType = PrevArgType.NONE;
                    continue;
                }
                case JAR -> {
                    jarFile = arg;
                    runArgsBoolean = true;
                    prevArgType = PrevArgType.NONE;
                    continue;
                }
            }
            if(arg.equals("--module-path") || arg.equals("-p")) {
                prevArgType = PrevArgType.MODULE_PATH;
                continue;
            }
            if(arg.equals("--classpath") || arg.equals("-cp")) {
                prevArgType = PrevArgType.CLASSPATH;
                continue;
            }
            if(arg.equals("--add-modules")) {
                prevArgType = PrevArgType.ADD_MODULES;
                continue;
            }
            if(arg.equals("--add-opens")) {
                prevArgType = PrevArgType.ADD_OPENS;
                continue;
            }
            if(arg.equals("--add-exports")) {
                prevArgType = PrevArgType.ADD_EXPORTS;
                continue;
            }
            if(arg.equals("--add-reads")) {
                prevArgType = PrevArgType.ADD_READS;
                continue;
            }
            if(arg.equals("--module") || arg.equals("-m")) {
                prevArgType = PrevArgType.MODULE;
                continue;
            }
            if(arg.equals("-jar")) {
                prevArgType = PrevArgType.JAR;
                continue;
            }
            jvmArgs.add(arg);
        }
        return new ArgsParseResult(conf, classpath, jvmArgs, mainClass, mainModule, jarFile, args);
    }

    public static <K, V> V multimapFirstOrNullValue(K key, Map<K, List<V>> params) {
        List<V> list = params.getOrDefault(key, Collections.emptyList());
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    public record ArgsParseResult(LaunchOptions.ModuleConf conf, List<String> classpath, List<String> jvmArgs, String mainClass, String mainModule, String jarFile, List<String> args) {

    }

    private enum PrevArgType {
        NONE, MODULE_PATH, ADD_MODULES, ADD_OPENS, ADD_EXPORTS, ADD_READS, CLASSPATH, POST_CLASSPATH, JAR, MAINCLASS, MODULE;
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
