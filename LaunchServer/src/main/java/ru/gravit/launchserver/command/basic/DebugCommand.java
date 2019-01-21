package ru.gravit.launchserver.command.basic;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

public final class DebugCommand extends Command {
    public DebugCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[true/false] (true/false)";
    }

    @Override
    public String getUsageDescription() {
        return "Enable or disable debug and stacktrace logging at runtime";
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
