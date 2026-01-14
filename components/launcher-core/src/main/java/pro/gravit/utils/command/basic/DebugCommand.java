package pro.gravit.utils.command.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class DebugCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(DebugCommand.class);

    @Override
    public String getArgsDescription() {
        return "[true/false] [true/false]";
    }

    @Override
    public String getUsageDescription() {
        return null;
    }

    @Override
    public void invoke(String... args) {
        boolean newValue, newTraceValue;
        if (args.length >= 1) {
            newValue = Boolean.parseBoolean(args[0]);
            if (args.length >= 2) newTraceValue = Boolean.parseBoolean(args[1]);
            else newTraceValue = newValue;
            LogHelper.setDebugEnabled(newValue);
            LogHelper.setStacktraceEnabled(newTraceValue);
        } else {
            newValue = true;
            newTraceValue = true;
        }
        LogHelper.subInfo("Debug enabled: " + newValue);
        LogHelper.subInfo("Stacktrace enabled: " + newTraceValue);
    }
}