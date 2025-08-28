package pro.gravit.utils.logging;

import pro.gravit.utils.helper.LogHelper;

import java.util.function.Supplier;

public interface LogHelperAppender {
    void log(LogHelper.Level level, String message, boolean sub);

    void logJAnsi(LogHelper.Level level, Supplier<String> plaintext, Supplier<String> jansitext, boolean sub);

    boolean isDebugEnabled();

    void setDebugEnabled(boolean debugEnabled);

    boolean isStacktraceEnabled();

    void setStacktraceEnabled(boolean stacktraceEnabled);

    boolean isDevEnabled();

    void setDevEnabled(boolean stacktraceEnabled);

    void addOutput(LogHelper.OutputEnity output);

    boolean removeOutput(LogHelper.OutputEnity output);

    void printVersion(String product);

    void printLicense(String product);
}
