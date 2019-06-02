package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class ReloadCommand extends Command {
    public ReloadCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[name]";
    }

    @Override
    public String getUsageDescription() {
        return "Reload provider/handler/module config";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        LogHelper.info("Reload %s config", args[0]);
        server.reloadManager.reload(args[0]);
    }
}
