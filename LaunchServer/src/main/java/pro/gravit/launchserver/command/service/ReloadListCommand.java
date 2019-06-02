package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

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
