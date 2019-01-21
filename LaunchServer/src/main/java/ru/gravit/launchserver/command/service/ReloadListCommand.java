package ru.gravit.launchserver.command.service;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

public class ReloadListCommand extends Command {
    public ReloadListCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "";
    }

    @Override
    public String getUsageDescription() {
        return "print reloadable configs";
    }

    @Override
    public void invoke(String... args) {
        server.reloadManager.printReloadables();
    }
}
