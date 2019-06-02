package pro.gravit.launchserver.command.service;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class GivePermissionsCommand extends Command {
    public GivePermissionsCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[username] [permission] [true/false]";
    }

    @Override
    public String getUsageDescription() {
        return "give permissions";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        String username = args[0];
        ClientPermissions permissions = server.config.permissionsHandler.getPermissions(username);
        String permission = args[1];
        boolean isEnabled = Boolean.valueOf(args[2]);
        switch (permission) {
            case "admin": {
                permissions.canAdmin = isEnabled;
                break;
            }
            case "server": {
                permissions.canServer = isEnabled;
                break;
            }
            case "bot": {
                permissions.canBot = isEnabled;
                break;
            }
            default: {
                LogHelper.error("Unknown permission: %s", permission);
                return;
            }
        }
        LogHelper.info("Write new permissions for %s", username);
        server.config.permissionsHandler.setPermissions(username, permissions);
    }
}
