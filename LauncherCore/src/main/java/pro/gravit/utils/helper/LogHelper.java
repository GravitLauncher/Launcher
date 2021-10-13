package pro.gravit.utils.helper;

import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.utils.logging.LogHelperAppender;
import pro.gravit.utils.logging.SimpleLogHelperImpl;
import pro.gravit.utils.logging.Slf4jLogHelperImpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LogHelper {

    public static final String DEBUG_PROPERTY = "launcher.debug";
    public static final String DEV_PROPERTY = "launcher.dev";
    public static final String STACKTRACE_PROPERTY = "launcher.stacktrace";
    public static final String NO_JANSI_PROPERTY = "launcher.noJAnsi";
    public static final String NO_SLF4J_PROPERTY = "launcher.noSlf4j";
    private static final Set<Consumer<Throwable>> EXCEPTIONS_CALLBACKS = Collections.newSetFromMap(new ConcurrentHashMap<>(2));
    private static LogHelperAppender impl;

    static {
        boolean useSlf4j = false;
        try {
            Class.forName("org.slf4j.Logger", false, LogHelper.class.getClassLoader());
            Class.forName("org.slf4j.impl.StaticLoggerBinder", false, LogHelper.class.getClassLoader());
            useSlf4j = !Boolean.getBoolean(NO_SLF4J_PROPERTY);
        } catch (ClassNotFoundException ignored) {
        }
        if (useSlf4j) {
            impl = new Slf4jLogHelperImpl();
        } else {
            impl = new SimpleLogHelperImpl();
        }
    }

    private LogHelper() {
    }

    public static void addOutput(OutputEnity output) {
        impl.addOutput(output);
    }

    public static void addExcCallback(Consumer<Throwable> output) {
        EXCEPTIONS_CALLBACKS.add(Objects.requireNonNull(output, "output"));
    }

    public static void addOutput(Output output, OutputTypes type) {
        addOutput(new OutputEnity(Objects.requireNonNull(output, "output"), type));
    }

    public static void addOutput(Path file) throws IOException {
        addOutput(IOHelper.newWriter(file, true));
    }

    public static void addOutput(Writer writer) {
        addOutput(new SimpleLogHelperImpl.WriterOutput(writer), OutputTypes.PLAIN);
    }

    public static void debug(String message) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, message, false);
        }
    }

    public static void dev(String message) {
        if (isDevEnabled()) {
            log(Level.DEV, message, false);
        }
    }

    public static void debug(String format, Object... args) {
        debug(String.format(format, args));
    }

    public static void dev(String format, Object... args) {
        if (isDevEnabled()) {
            dev(String.format(format, args));
        }
    }

    public static void error(Throwable exc) {
        EXCEPTIONS_CALLBACKS.forEach(e -> e.accept(exc));
        error(isStacktraceEnabled() ? toString(exc) : exc.toString());
    }

    public static void error(String message) {
        log(Level.ERROR, message, false);
    }

    public static void error(String format, Object... args) {
        error(String.format(format, args));
    }

    public static void info(String message) {
        log(Level.INFO, message, false);
    }

    public static void info(String format, Object... args) {
        info(String.format(format, args));
    }

    public static boolean isDebugEnabled() {
        return impl.isDebugEnabled();
    }

    public static void setDebugEnabled(boolean debugEnabled) {
        impl.setDebugEnabled(debugEnabled);
    }

    public static boolean isStacktraceEnabled() {
        return impl.isStacktraceEnabled();
    }

    public static void setStacktraceEnabled(boolean stacktraceEnabled) {
        impl.setStacktraceEnabled(stacktraceEnabled);
    }

    public static boolean isDevEnabled() {
        return impl.isDevEnabled();
    }

    public static void setDevEnabled(boolean stacktraceEnabled) {
        impl.setDevEnabled(stacktraceEnabled);
    }

    public static void log(Level level, String message, boolean sub) {
        impl.log(level, message, sub);
    }

    public static void logJAnsi(LogHelper.Level level, Supplier<String> plaintext, Supplier<String> jansitext, boolean sub) {
        impl.logJAnsi(level, plaintext, jansitext, sub);
    }

    public static void printVersion(String product) {
        impl.printVersion(product);
    }

    public static void printLicense(String product) {
        impl.printLicense(product);
    }

    public static boolean removeOutput(OutputEnity output) {
        return impl.removeOutput(output);
    }

    public static void subDebug(String message) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, message, true);
        }
    }

    public static void subDebug(String format, Object... args) {
        subDebug(String.format(format, args));
    }

    public static void subInfo(String message) {
        log(Level.INFO, message, true);
    }

    public static void subInfo(String format, Object... args) {
        subInfo(String.format(format, args));
    }

    public static void subWarning(String message) {
        log(Level.WARNING, message, true);
    }

    public static void subWarning(String format, Object... args) {
        subWarning(String.format(format, args));
    }

    public static String toString(Throwable exc) {
        StringWriter sw = new StringWriter();
        exc.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static void warning(String message) {
        log(Level.WARNING, message, false);
    }

    public static void warning(String format, Object... args) {
        warning(String.format(format, args));
    }

    public enum OutputTypes {
        @LauncherNetworkAPI
        PLAIN,
        @LauncherNetworkAPI
        JANSI,
    }

    public enum Level {
        DEV("DEV"), DEBUG("DEBUG"), INFO("INFO"), WARNING("WARN"), ERROR("ERROR");
        public final String name;

        Level(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }


    @FunctionalInterface
    public interface Output {
        void println(String message);
    }

    public static class OutputEnity {
        public final Output output;
        public final OutputTypes type;

        public OutputEnity(Output output, OutputTypes type) {
            this.output = output;
            this.type = type;
        }
    }
}