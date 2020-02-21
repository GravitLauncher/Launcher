package pro.gravit.utils.command.basic;

import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class DebugCommand extends Command {
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
            newValue = LogHelper.isDebugEnabled();
            newTraceValue = LogHelper.isStacktraceEnabled();
        }
        LogHelper.subInfo("Debug enabled: " + newValue);
        LogHelper.subInfo("Stacktrace enabled: " + newTraceValue);
    }
}
