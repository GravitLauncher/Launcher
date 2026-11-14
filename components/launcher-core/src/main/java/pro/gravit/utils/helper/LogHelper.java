package pro.gravit.utils.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Required for old code, remove only after 5.8.0 release
@Deprecated(forRemoval = true)
public class LogHelper {
    private static final Logger logger = LoggerFactory.getLogger(LogHelper.class);
    public static void error(Throwable e) {
        logger.error("Error", e);

    }

    public static void debug(String format, Object... args) {
        logger.debug("{}", args == null || args.length == 0 ? format : String.format(format, args));
    }
}
