package ru.gravit.utils.helper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import ru.gravit.launcher.LauncherAPI;

@LauncherAPI
public class EnvHelper {
    private static final boolean TST;
    private static final boolean HASXM;
    @LauncherAPI
    public static final String[] toTest;
    @LauncherAPI
    public static final Pattern[] test;

    static {
        toTest = new String[]{"_JAVA_OPTIONS", "_JAVA_OPTS", "JAVA_OPTS", "JAVA_OPTIONS"};
        test = new Pattern[]{Pattern.compile("-Xm.*\\d+[KMG]")};
        TST = check0();
        HASXM = check1();
    }

    @LauncherAPI
    public static void addEnv(ProcessBuilder builder) {
        if (hasOptsVar()) {
            Map<String, String> repl = new HashMap<>();
            for (String str : toTest) {
                repl.put(str, "");
                repl.put(str.toLowerCase(Locale.ENGLISH), "");
            }
            JVMHelper.appendVars(builder, repl);
        }
    }

    private static boolean check0() {
        for (String test : toTest) if (System.getProperty(test) != null) return true;
        return false;
    }

    /**
     * Вынужденное решение ибо тест на наличие -Xm* этакой нужен.
     */
    private static boolean check1() {
        if (hasOptsVar())
			for (String testStr : toTest)
                if (System.getProperty(testStr) != null) {
                    String str = System.getenv(testStr);
                    StringTokenizer st = new StringTokenizer(str);
                    while (st.hasMoreTokens())
                        if (CommonHelper.multiMatches(test, st.nextToken())) return true;
                }
        return false;
    }

    @LauncherAPI
    public static boolean hasMemPreDef() {
        return HASXM;
    }

    @LauncherAPI
    public static boolean hasOptsVar() {
        return TST;
    }
}
