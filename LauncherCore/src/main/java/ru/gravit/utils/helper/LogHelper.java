package ru.gravit.utils.helper;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiOutputStream;
import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.LauncherNetworkAPI;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LogHelper {
    @LauncherAPI
    public static final String DEBUG_PROPERTY = "launcher.debug";
    @LauncherAPI
    public static final String DEV_PROPERTY = "launcher.dev";
    @LauncherAPI
    public static final String STACKTRACE_PROPERTY = "launcher.stacktrace";
    @LauncherAPI
    public static final String NO_JANSI_PROPERTY = "launcher.noJAnsi";
    @LauncherAPI
    public static final boolean JANSI;

    // Output settings
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss", Locale.US);
    private static final AtomicBoolean DEBUG_ENABLED = new AtomicBoolean(Boolean.getBoolean(DEBUG_PROPERTY));
    private static final AtomicBoolean STACKTRACE_ENABLED = new AtomicBoolean(Boolean.getBoolean(STACKTRACE_PROPERTY));
    private static final AtomicBoolean DEV_ENABLED = new AtomicBoolean(Boolean.getBoolean(DEV_PROPERTY));

    public static class OutputEnity {
        public Output output;
        public OutputTypes type;

        public OutputEnity(Output output, OutputTypes type) {
            this.output = output;
            this.type = type;
        }
    }

    public enum OutputTypes {
        @LauncherNetworkAPI
        PLAIN,
        @LauncherNetworkAPI
        JANSI,
        @LauncherNetworkAPI
        HTML
    }

    private static final Set<OutputEnity> OUTPUTS = Collections.newSetFromMap(new ConcurrentHashMap<>(2));
    private static final Set<Consumer<Throwable>> EXCEPTIONS_CALLBACKS = Collections.newSetFromMap(new ConcurrentHashMap<>(2));
    private static final OutputEnity STD_OUTPUT;

    private LogHelper() {
    }

    @LauncherAPI
    public static void addOutput(OutputEnity output) {
        OUTPUTS.add(Objects.requireNonNull(output, "output"));
    }

    @LauncherAPI
    public static void addExcCallback(Consumer<Throwable> output) {
        EXCEPTIONS_CALLBACKS.add(Objects.requireNonNull(output, "output"));
    }

    @LauncherAPI
    public static void addOutput(Output output, OutputTypes type) {
        OUTPUTS.add(new OutputEnity(Objects.requireNonNull(output, "output"), type));
    }

    @LauncherAPI
    public static void addOutput(Path file) throws IOException {
        if (JANSI) {
            addOutput(new JAnsiOutput(IOHelper.newOutput(file, true)), OutputTypes.JANSI);
        } else {
            addOutput(IOHelper.newWriter(file, true));
        }
    }

    @LauncherAPI
    public static void addOutput(Writer writer) {
        addOutput(new WriterOutput(writer), OutputTypes.PLAIN);
    }

    @LauncherAPI
    public static void debug(String message) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, message, false);
        }
    }

    @LauncherAPI
    public static void dev(String message) {
        if (isDevEnabled()) {
            log(Level.DEV, message, false);
        }
    }

    @LauncherAPI
    public static void debug(String format, Object... args) {
        debug(String.format(format, args));
    }

    @LauncherAPI
    public static void dev(String format, Object... args) {
        if(isDevEnabled())
        {
            dev(String.format(format, args));
        }
    }

    @LauncherAPI
    public static void error(Throwable exc) {
        EXCEPTIONS_CALLBACKS.forEach(e -> e.accept(exc));
        error(isStacktraceEnabled() ? toString(exc) : exc.toString());
    }

    @LauncherAPI
    public static void error(String message) {
        log(Level.ERROR, message, false);
    }

    @LauncherAPI
    public static void error(String format, Object... args) {
        error(String.format(format, args));
    }

    @LauncherAPI
    public static void info(String message) {
        log(Level.INFO, message, false);
    }

    @LauncherAPI
    public static void info(String format, Object... args) {
        info(String.format(format, args));
    }

    @LauncherAPI
    public static boolean isDebugEnabled() {
        return DEBUG_ENABLED.get();
    }

    @LauncherAPI
    public static void setDebugEnabled(boolean debugEnabled) {
        DEBUG_ENABLED.set(debugEnabled);
    }

    @LauncherAPI
    public static boolean isStacktraceEnabled() {
        return STACKTRACE_ENABLED.get();
    }

    @LauncherAPI
    public static boolean isDevEnabled() {
        return DEV_ENABLED.get();
    }

    @LauncherAPI
    public static void setStacktraceEnabled(boolean stacktraceEnabled) {
        STACKTRACE_ENABLED.set(stacktraceEnabled);
    }

    @LauncherAPI
    public static void setDevEnabled(boolean stacktraceEnabled) {
        DEV_ENABLED.set(stacktraceEnabled);
    }

    public static String getDataTime() {
        return DATE_TIME_FORMATTER.format(LocalDateTime.now());
    }

    @LauncherAPI
    public static void log(Level level, String message, boolean sub) {
        String dateTime = DATE_TIME_FORMATTER.format(LocalDateTime.now());
        String jansiString = null, plainString = null, htmlString = null;
        for (OutputEnity output : OUTPUTS) {
            if (output.type == OutputTypes.JANSI && JANSI) {
                if (jansiString != null) {
                    output.output.println(jansiString);
                    continue;
                }

                jansiString = ansiFormatLog(level, dateTime, message, sub);
                output.output.println(jansiString);
            } else if (output.type == OutputTypes.HTML) {
                if (htmlString != null) {
                    output.output.println(htmlString);
                    continue;
                }

                htmlString = htmlFormatLog(level, dateTime, message, sub);
                output.output.println(htmlString);
            } else {
                if (plainString != null) {
                    output.output.println(plainString);
                    continue;
                }

                plainString = formatLog(level, message, dateTime, sub);
                output.output.println(plainString);
            }
        }
    }

    @LauncherAPI
    public static void rawLog(Supplier<String> plainStr, Supplier<String> jansiStr) {
        rawLog(plainStr, jansiStr, null);
    }

    @LauncherAPI
    public static void rawLog(Supplier<String> plainStr, Supplier<String> jansiStr, Supplier<String> htmlStr) {
        String jansiString = null, plainString = null, htmlString = null;
        for (OutputEnity output : OUTPUTS) {
            if (output.type == OutputTypes.JANSI && JANSI) {
                if (jansiString != null) {
                    output.output.println(jansiString);
                    continue;
                }

                jansiString = jansiStr.get();
                output.output.println(jansiString);
            } else if (output.type == OutputTypes.HTML) {
                if (htmlString != null) {
                    output.output.println(htmlString);
                    continue;
                }

                htmlString = htmlStr.get();
                output.output.println(htmlString);
            } else {
                if (plainString != null) {
                    output.output.println(plainString);
                    continue;
                }

                plainString = plainStr.get();
                output.output.println(plainString);
            }
        }
    }

    @LauncherAPI
    public static void printVersion(String product) {
        String jansiString = null, plainString = null;
        for (OutputEnity output : OUTPUTS) {
            if (output.type == OutputTypes.JANSI && JANSI) {
                if (jansiString != null) {
                    output.output.println(jansiString);
                    continue;
                }

                jansiString = FormatHelper.ansiFormatVersion(product);
                output.output.println(jansiString);
            } else {
                if (plainString != null) {
                    output.output.println(plainString);
                    continue;
                }

                plainString = FormatHelper.formatVersion(product);
                output.output.println(plainString);
            }
        }
    }

    @LauncherAPI
    public static void printLicense(String product) {
        String jansiString = null, plainString = null;
        for (OutputEnity output : OUTPUTS) {
            if (output.type == OutputTypes.JANSI && JANSI) {
                if (jansiString != null) {
                    output.output.println(jansiString);
                    continue;
                }

                jansiString = FormatHelper.ansiFormatLicense(product);
                output.output.println(jansiString);
            } else {
                if (plainString != null) {
                    output.output.println(plainString);
                    continue;
                }

                plainString = FormatHelper.formatLicense(product);
                output.output.println(plainString);
            }
        }
    }

    @LauncherAPI
    public static boolean removeOutput(OutputEnity output) {
        return OUTPUTS.remove(output);
    }

    @LauncherAPI
    public static boolean removeStdOutput() {
        return removeOutput(STD_OUTPUT);
    }

    @LauncherAPI
    public static void subDebug(String message) {
        if (isDebugEnabled()) {
            log(Level.DEBUG, message, true);
        }
    }

    @LauncherAPI
    public static void subDebug(String format, Object... args) {
        subDebug(String.format(format, args));
    }

    @LauncherAPI
    public static void subInfo(String message) {
        log(Level.INFO, message, true);
    }

    @LauncherAPI
    public static void subInfo(String format, Object... args) {
        subInfo(String.format(format, args));
    }

    @LauncherAPI
    public static void subWarning(String message) {
        log(Level.WARNING, message, true);
    }

    @LauncherAPI
    public static void subWarning(String format, Object... args) {
        subWarning(String.format(format, args));
    }

    @LauncherAPI
    public static String toString(Throwable exc) {
        try (StringWriter sw = new StringWriter()) {
            try (PrintWriter pw = new PrintWriter(sw)) {
                exc.printStackTrace(pw);
            }
            return sw.toString();
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }

    @LauncherAPI
    public static void warning(String message) {
        log(Level.WARNING, message, false);
    }

    @LauncherAPI
    public static void warning(String format, Object... args) {
        warning(String.format(format, args));
    }

    private static String ansiFormatLog(Level level, String dateTime, String message, boolean sub) {

        Ansi ansi = FormatHelper.rawAnsiFormat(level, dateTime, sub);
        ansi.a(message);

        // Finish with reset code
        return ansi.reset().toString();
    }

    public static String htmlFormatLog(Level level, String dateTime, String message, boolean sub) {
        String levelColor;
        switch (level) {
            case WARNING:
                levelColor = "gravitlauncher-log-warning";
                break;
            case ERROR:
                levelColor = "gravitlauncher-log-error";
                break;
            case INFO:
                levelColor = "gravitlauncher-log-info";
                break;
            case DEBUG:
                levelColor = "gravitlauncher-log-debug";
                break;
            case DEV:
                levelColor = "gravitlauncher-log-dev";
                break;
            default:
                levelColor = "gravitlauncher-log-unknown";
                break;
        }
        if (sub) levelColor += " gravitlauncher-log-sub";
        return String.format("%s <span class=\"gravitlauncher-log %s\">[%s] %s</span>", dateTime, levelColor, level.toString(), sub ? ' ' + message : message);
    }

    private static String formatLog(Level level, String message, String dateTime, boolean sub) {
        return FormatHelper.rawFormat(level, dateTime, sub) + message;
    }

    static {
        // Use JAnsi if available
        boolean jansi;
        try {
            if (Boolean.getBoolean(NO_JANSI_PROPERTY)) {
                jansi = false;
            } else {
                Class.forName("org.fusesource.jansi.Ansi");
                AnsiConsole.systemInstall();
                jansi = true;
            }
        } catch (ClassNotFoundException ignored) {
            jansi = false;
        }
        JANSI = jansi;

        // Add std writer
        STD_OUTPUT = new OutputEnity(System.out::println, JANSI ? OutputTypes.JANSI : OutputTypes.PLAIN);
        addOutput(STD_OUTPUT);

        // Add file log writer
        String logFile = System.getProperty("launcher.logFile");
        if (logFile != null) {
            try {
                addOutput(IOHelper.toPath(logFile));
            } catch (IOException e) {
                error(e);
            }
        }
    }

    @LauncherAPI
    @FunctionalInterface
    public interface Output {
        void println(String message);
    }

    @LauncherAPI
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

    private static final class JAnsiOutput extends WriterOutput {
        private JAnsiOutput(OutputStream output) {
            super(IOHelper.newWriter(new AnsiOutputStream(output)));
        }
    }

    private static class WriterOutput implements Output, AutoCloseable {
        private final Writer writer;

        private WriterOutput(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        @Override
        public void println(String message) {
            try {
                writer.write(message + System.lineSeparator());
                writer.flush();
            } catch (IOException ignored) {
                // Do nothing?
            }
        }
    }
}