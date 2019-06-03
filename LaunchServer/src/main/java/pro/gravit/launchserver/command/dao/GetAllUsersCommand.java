package pro.gravit.launchserver.command.dao;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.dao.User;
import pro.gravit.utils.helper.LogHelper;

public class GetAllUsersCommand extends Command {
    public GetAllUsersCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "";
    }

    @Override
    public String getUsageDescription() {
        return "get all users information";
    }

    @Override
    public void invoke(String... args) throws Exception {
        int count = 0;
        for(User user : server.userService.findAllUsers())
        {
            LogHelper.subInfo("[%s] UUID: %s", user.username, user.uuid.toString());
            count++;
        }
        LogHelper.info("Print %d users", count);
    }
}
