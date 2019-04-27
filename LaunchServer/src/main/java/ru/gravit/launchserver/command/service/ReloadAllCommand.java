package ru.gravit.launchserver.command.service;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

public class ReloadAllCommand extends Command {
    public ReloadAllCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "";
    }

    @Override
    public String getUsageDescription() {
        return "Reload all provider/handler/module config";
    }

    @Override
    public void invoke(String... args) throws Exception {
        LogHelper.info("Reload all config");
        server.reloadManager.reloadAll();
    }
}
