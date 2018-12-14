package ru.gravit.launchserver.command.basic;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

public final class LogConnectionsCommand extends Command {
    public LogConnectionsCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[true/false]";
    }

    @Override
    public String getUsageDescription() {
        return "Enable or disable logging connections";
    }

    @Override
    public void invoke(String... args) {
        boolean newValue;
        if (args.length >= 1) {
            newValue = Boolean.parseBoolean(args[0]);
            server.serverSocketHandler.logConnections = newValue;
        } else
            newValue = server.serverSocketHandler.logConnections;
        LogHelper.subInfo("Log connections: " + newValue);
    }
}
