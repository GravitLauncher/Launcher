package pro.gravit.launchserver;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.jar.JarFile;

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
