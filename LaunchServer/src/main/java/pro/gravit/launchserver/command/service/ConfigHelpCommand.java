package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class ConfigHelpCommand extends Command {
    public ConfigHelpCommand(LaunchServer server) {
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
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        LogHelper.info("Help %s module", args[0]);
        server.reconfigurableManager.printHelp(args[0]);
    }
}
