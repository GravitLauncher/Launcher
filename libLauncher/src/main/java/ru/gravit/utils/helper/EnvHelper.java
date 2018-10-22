package ru.gravit.utils.helper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class EnvHelper {
	public static final String[] toTest;

	static {
		toTest = new String[] { "_JAVA_OPTIONS", "_JAVA_OPTS", "JAVA_OPTS", "JAVA_OPTIONS" };
	}

	public static void addEnv(ProcessBuilder builder) {
		Map<String, String> repl = builder.environment();
		for (String str : toTest) {
			repl.put(str, "");
			repl.put(str.toLowerCase(Locale.US), "");
		}
	}

	public static void checkDangerousParametrs() {
		for (String t : toTest)
			if (System.getenv(t) != null) {
				String env = System.getenv(t).toLowerCase(Locale.US);
				if (env.contains("-cp") || env.contains("-classpath") || env.contains("-javaagent")
						|| env.contains("-agentpath") || env.contains("-agentlib"))
					throw new SecurityException("JavaAgent in global optings not allow");
			}
	}
}
