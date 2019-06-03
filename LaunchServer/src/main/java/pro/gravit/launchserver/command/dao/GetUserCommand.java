package pro.gravit.launchserver.command.dao;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.dao.User;
import pro.gravit.utils.helper.LogHelper;

public class GetUserCommand extends Command {
    public GetUserCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[username]";
    }

    @Override
    public String getUsageDescription() {
        return "get user information";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        User user = server.userService.findUserByUsername(args[0]);
        if(user == null)
        {
            LogHelper.error("User %s not found", args[0]);
            return;
        }
        LogHelper.info("[%s] UUID: %s", user.username, user.uuid.toString());
    }
}
