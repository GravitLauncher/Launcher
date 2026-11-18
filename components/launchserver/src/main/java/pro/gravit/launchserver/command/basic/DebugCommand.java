package pro.gravit.launchserver.command.basic;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.util.function.Consumer;

public class DebugCommand extends Command {
    private final transient Logger logger = LogManager.getLogger();

    private final Consumer<Boolean> setTrace;

    public DebugCommand(LaunchServer server) {
        this(server, DebugCommand::setStandaloneTrace);
    }

    public DebugCommand(LaunchServer server, Consumer<Boolean> setTrace) {
        super(server);
        this.setTrace = setTrace;
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
        setTrace.accept(value);
        logger.info("Log level TRACE {}", value ? "enabled" : "disabled");
    }

    private static void setStandaloneTrace(boolean value) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        config.getWatchManager().setIntervalSeconds(-1);
        LoggerConfig loggerConfig = config.getLoggerConfig("pro.gravit");
        loggerConfig.setLevel(value ? Level.TRACE : Level.DEBUG);
        ctx.updateLoggers();
    }
}
