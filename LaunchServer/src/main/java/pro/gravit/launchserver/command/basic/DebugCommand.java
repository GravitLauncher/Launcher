package pro.gravit.launchserver.command.basic;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class DebugCommand extends Command {
    private transient Logger logger = LogManager.getLogger();
    public DebugCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[true/false]";
    }

    @Override
    public String getUsageDescription() {
        return "Enable log level TRACE in LaunchServer";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        boolean value = Boolean.parseBoolean(args[0]);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("pro.gravit");
        loggerConfig.setLevel(value ? Level.TRACE : Level.DEBUG);
        ctx.updateLoggers();
        if(value) {
            logger.info("Log level TRACE enabled");
        } else {
            logger.info("Log level TRACE disabled");
        }
    }
}
