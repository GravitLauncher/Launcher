package pro.gravit.utils.logging;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import pro.gravit.utils.helper.FormatHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static pro.gravit.utils.helper.LogHelper.*;

public class SimpleLogHelperImpl implements LogHelperAppender {

    private static final AtomicBoolean DEBUG_ENABLED = new AtomicBoolean(Boolean.getBoolean(DEBUG_PROPERTY));
    private static final AtomicBoolean STACKTRACE_ENABLED = new AtomicBoolean(Boolean.getBoolean(STACKTRACE_PROPERTY));
    private static final AtomicBoolean DEV_ENABLED = new AtomicBoolean(Boolean.getBoolean(DEV_PROPERTY));
    public final boolean JANSI;
    // Output settings
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss", Locale.US);
    private final Set<LogHelper.OutputEnity> OUTPUTS = Collections.newSetFromMap(new ConcurrentHashMap<>(2));
    private final LogHelper.OutputEnity STD_OUTPUT;

    public SimpleLogHelperImpl() {
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
        STD_OUTPUT = new LogHelper.OutputEnity(System.out::println, JANSI ? LogHelper.OutputTypes.JANSI : LogHelper.OutputTypes.PLAIN);
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

    public void addOutput(Writer writer) {
        addOutput(new WriterOutput(writer), OutputTypes.PLAIN);
    }


    public void log(Level level, String message, boolean sub) {
        String dateTime = DATE_TIME_FORMATTER.format(LocalDateTime.now());
        String jansiString = null, plainString = null;
        for (OutputEnity output : OUTPUTS) {
            if (output.type == OutputTypes.JANSI && JANSI) {
                if (jansiString != null) {
                    output.output.println(jansiString);
                    continue;
                }

                jansiString = ansiFormatLog(level, dateTime, message, sub);
                output.output.println(jansiString);
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

    @Override
    public void logJAnsi(Level level, Supplier<String> plaintext, Supplier<String> jansitext, boolean sub) {
        if (JANSI) {
            log(level, jansitext.get(), sub);
        } else {
            log(level, plaintext.get(), sub);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return DEBUG_ENABLED.get();
    }

    @Override
    public void setDebugEnabled(boolean debugEnabled) {
        DEBUG_ENABLED.set(debugEnabled);
    }

    @Override
    public boolean isStacktraceEnabled() {
        return STACKTRACE_ENABLED.get();
    }

    @Override
    public void setStacktraceEnabled(boolean stacktraceEnabled) {
        STACKTRACE_ENABLED.set(stacktraceEnabled);
    }

    @Override
    public boolean isDevEnabled() {
        return DEV_ENABLED.get();
    }

    @Override
    public void setDevEnabled(boolean stacktraceEnabled) {
        DEV_ENABLED.set(stacktraceEnabled);
    }

    @Override
    public void addOutput(OutputEnity output) {
        OUTPUTS.add(output);
    }

    @Override
    public boolean removeOutput(OutputEnity output) {
        return OUTPUTS.remove(output);
    }

    public void rawLog(Supplier<String> plainStr, Supplier<String> jansiStr) {
        String jansiString = null, plainString = null;
        for (OutputEnity output : OUTPUTS) {
            if (output.type == OutputTypes.JANSI && JANSI) {
                if (jansiString != null) {
                    output.output.println(jansiString);
                    continue;
                }

                jansiString = jansiStr.get();
                output.output.println(jansiString);
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

    public void addOutput(Output output, OutputTypes type) {
        addOutput(new OutputEnity(Objects.requireNonNull(output, "output"), type));
    }

    public void addOutput(Path file) throws IOException {
        if (JANSI) {
            addOutput(new JAnsiOutput(IOHelper.newOutput(file, true)), OutputTypes.JANSI);
        } else {
            addOutput(IOHelper.newWriter(file, true));
        }
    }


    private String ansiFormatLog(Level level, String dateTime, String message, boolean sub) {

        Ansi ansi = FormatHelper.rawAnsiFormat(level, dateTime, sub);
        ansi.a(message);

        // Finish with reset code
        return ansi.reset().toString();
    }

    private String formatLog(Level level, String message, String dateTime, boolean sub) {
        return FormatHelper.rawFormat(level, dateTime, sub) + message;
    }

    public void printVersion(String product) {
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

    public void printLicense(String product) {
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

    public static final class JAnsiOutput extends WriterOutput {
        private JAnsiOutput(OutputStream output) {
            super(IOHelper.newWriter(output));
        }
    }


    public static class WriterOutput implements Output, AutoCloseable {
        private final Writer writer;

        public WriterOutput(Writer writer) {
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
