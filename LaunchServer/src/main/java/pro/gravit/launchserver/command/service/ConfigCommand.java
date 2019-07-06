package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.manangers.ReconfigurableManager;
import pro.gravit.utils.helper.LogHelper;

public class ConfigCommand extends Command {
    public ConfigCommand(LaunchServer server) {
        super(server.reconfigurableManager.getCommands(), server);
    }

    @Override
    public String getArgsDescription() {
        return "[name] [action] [more args]";
    }

    @Override
    public String getUsageDescription() {
        return "call reconfigurable action";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}
