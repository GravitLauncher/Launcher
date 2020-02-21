package pro.gravit.launchserver.command.dao;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.dao.User;
import pro.gravit.utils.helper.LogHelper;

public class SetUserPasswordCommand extends Command {

    public SetUserPasswordCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[username] [new password]";
    }

    @Override
    public String getUsageDescription() {
        return "Set user password";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        User user = server.config.dao.userDAO.findByUsername(args[0]);
        if (user == null) {
            LogHelper.error("User %s not found", args[1]);
            return;
        }
        user.setPassword(args[1]);
        server.config.dao.userDAO.update(user);
        LogHelper.info("[%s] UUID: %s | New Password: %s", user.getUsername(), user.getUuid().toString(), args[1]);
    }
}
