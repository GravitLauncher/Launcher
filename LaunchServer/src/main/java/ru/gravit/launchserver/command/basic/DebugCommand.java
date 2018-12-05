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
        return "[true/false]";
    }

    @Override
    public String getUsageDescription() {
        return "Enable or disable debug logging at runtime";
    }

    @Override
    public void invoke(String... args) {
        boolean newValue;
        if (args.length >= 1) {
            newValue = Boolean.parseBoolean(args[0]);
            LogHelper.setDebugEnabled(newValue);
        } else
            newValue = LogHelper.isDebugEnabled();
        LogHelper.subInfo("Debug enabled: " + newValue);
    }
}
