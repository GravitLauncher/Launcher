package pro.gravit.launchserver.command.service;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class ConfigCommand extends Command {
    public ConfigCommand(LaunchServer server) {
        super(server);
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
        verifyArgs(args, 2);
        LogHelper.info("Call %s module %s action", args[0], args[1]);
        String[] new_args = new String[args.length - 2];
        System.arraycopy(args, 2, new_args, 0, args.length - 2);
        server.reconfigurableManager.call(args[0], args[1], new_args);
    }
}
