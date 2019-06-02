package pro.gravit.launchserver.command.service;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class GetPermissionsCommand extends Command {
    public GetPermissionsCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[username]";
    }

    @Override
    public String getUsageDescription() {
        return "print username permissions";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        String username = args[0];
        ClientPermissions permissions = server.config.permissionsHandler.getPermissions(username);
        LogHelper.info("Permissions %s: %s (long: %d)", username, permissions.toString(), permissions.toLong());
    }
}
