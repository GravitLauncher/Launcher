package ru.gravit.launchserver.command.service;

import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

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
