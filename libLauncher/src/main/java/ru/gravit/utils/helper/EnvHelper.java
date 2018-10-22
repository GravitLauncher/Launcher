package ru.gravit.utils.helper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class EnvHelper {
	private static final boolean TST;
	private static final boolean HASXW;
	public static final String[] toTest;
	public static final Pattern[] test;

	static {
		toTest = new String[] { "_JAVA_OPTIONS", "_JAVA_OPTS", "JAVA_OPTS", "JAVA_OPTIONS" };
		test = new Pattern[] { Pattern.compile("-xm.*\\d+[KMG]") };
		TST = check0();
		HASXW = check1();
	}

	public static void addEnv(ProcessBuilder builder) {
		if (hasOptsEnv()) {
			Map<String, String> repl = new HashMap<>();
			for (String str : toTest) {
				repl.put(str, "");
				repl.put(str.toLowerCase(Locale.US), "");
			}
			JVMHelper.appendVars(builder, repl);
		}
	}

	private static boolean check0() {
		for (String test : toTest)
			if (System.getProperty(test) != null)
				return true;
		return false;
	}

	/**
	 * Вынужденное решение ибо тест на наличие -Xm* этакой нужен.
	 */
	private static boolean check1() {
		if (hasOptsEnv())
			for (String testStr : toTest)
				if (System.getenv(testStr) != null) {
					String str = System.getenv(testStr).toLowerCase(Locale.US);
					StringTokenizer st = new StringTokenizer(str);
					while (st.hasMoreTokens())
						if (CommonHelper.multiMatches(test, st.nextToken()))
							return true;
				}
		return false;
	}

	public static void checkDangerousParametrs() {
		if (hasOptsEnv())
			for (String t : toTest)
				if (System.getenv(t) != null) {
					String env = System.getenv(t).toLowerCase(Locale.US);
					if (env.contains("-cp") || env.contains("-classpath") || env.contains("-javaagent")
							|| env.contains("-agentpath") || env.contains("-agentlib"))
						throw new SecurityException("JavaAgent in global optings not allow");
				}
	}

	public static boolean hasOptsEnv() {
		return TST;
	}

	public static boolean hasWarnPreDef() {
		return HASXW;
	}
}
