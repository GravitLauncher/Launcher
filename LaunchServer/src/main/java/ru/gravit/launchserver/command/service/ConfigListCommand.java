package ru.gravit.launchserver.command.service;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

public class ConfigListCommand extends Command {
    public ConfigListCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[name]";
    }

    @Override
    public String getUsageDescription() {
        return "print help for config command";
    }

    @Override
    public void invoke(String... args) {
        server.reconfigurableManager.printReconfigurables();
    }
}
