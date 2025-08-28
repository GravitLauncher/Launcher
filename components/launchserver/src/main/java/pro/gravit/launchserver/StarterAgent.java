package pro.gravit.launchserver;

import java.lang.instrument.Instrumentation;
import java.nio.file.*;

public final class StarterAgent {

    public static Instrumentation inst = null;
    public static Path libraries = null;
    private static boolean isStarted = false;

    public static boolean isAgentStarted() {
        return isStarted;
    }

    public static void premain(String agentArgument, Instrumentation inst) {
        throw new UnsupportedOperationException("Please remove -javaagent option from start.sh");
    }
}
