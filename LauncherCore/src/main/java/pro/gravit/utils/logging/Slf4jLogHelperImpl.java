package pro.gravit.utils.logging;

import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.utils.helper.FormatHelper;
import pro.gravit.utils.helper.LogHelper;

import java.util.function.Supplier;

import static pro.gravit.utils.helper.LogHelper.NO_JANSI_PROPERTY;

public class Slf4jLogHelperImpl implements LogHelperAppender {
    private final Logger logger = LoggerFactory.getLogger("LogHelper");
    private final boolean JANSI;

    public Slf4jLogHelperImpl() {
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
    }

    @Override
    public void log(LogHelper.Level level, String message, boolean sub) {
        switch (level) {
            case DEV:
                logger.trace(message);
                break;
            case DEBUG:
                logger.debug(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case WARNING:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
        }
    }

    @Override
    public void logJAnsi(LogHelper.Level level, Supplier<String> plaintext, Supplier<String> jansitext, boolean sub) {
        if (JANSI) {
            log(level, jansitext.get(), sub);
        } else {
            log(level, plaintext.get(), sub);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void setDebugEnabled(boolean debugEnabled) {
        //NOP
    }

    @Override
    public boolean isStacktraceEnabled() {
        return true;
    }

    @Override
    public void setStacktraceEnabled(boolean stacktraceEnabled) {
        //NOP
    }

    @Override
    public boolean isDevEnabled() {
        return true;
    }

    @Override
    public void setDevEnabled(boolean stacktraceEnabled) {
        //NOP
    }

    @Override
    public void addOutput(LogHelper.OutputEnity output) {

    }

    @Override
    public boolean removeOutput(LogHelper.OutputEnity output) {
        return false;
    }

    @Override
    public void printVersion(String product) {
        if (JANSI) {
            logger.info(FormatHelper.ansiFormatVersion(product));
        } else {
            logger.info(FormatHelper.formatVersion(product));
        }
    }

    @Override
    public void printLicense(String product) {
        if (JANSI) {
            logger.info(FormatHelper.ansiFormatLicense(product));
        } else {
            logger.info(FormatHelper.formatLicense(product));
        }
    }
}
