package pro.gravit.utils.helper;

import java.util.Locale;
import java.util.Map;

public final class EnvHelper {
    public static final String[] toTest = {"_JAVA_OPTIONS", "_JAVA_OPTS", "JAVA_OPTS", "JAVA_OPTIONS"};

    public static void addEnv(ProcessBuilder builder) {
        Map<String, String> map = builder.environment();
        for (String env : toTest) {
            if (map.containsKey(env))
                map.put(env, "");
            String lower_env = env.toLowerCase(Locale.US);
            if (map.containsKey(lower_env))
                map.put(lower_env, "");
        }
    }

    public static void checkDangerousParams() {
        for (String t : toTest) {
            String env = System.getenv(t);
            if (env != null) {
                env = env.toLowerCase(Locale.US);
                if (env.contains("-cp") || env.contains("-classpath") || env.contains("-javaagent")
                        || env.contains("-agentpath") || env.contains("-agentlib"))
                    throw new SecurityException("JavaAgent in global options not allow");
            }
        }
    }
}
