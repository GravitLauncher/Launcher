package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

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
