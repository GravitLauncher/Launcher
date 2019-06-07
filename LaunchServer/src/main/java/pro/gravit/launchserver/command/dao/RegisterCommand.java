package pro.gravit.launchserver.command.dao;

import java.util.UUID;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.dao.User;
import pro.gravit.utils.helper.LogHelper;

public class RegisterCommand extends Command {
    public RegisterCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[login] [password]";
    }

    @Override
    public String getUsageDescription() {
        return "Register new user";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        User user = new User();
        user.username = args[0];
        user.setPassword(args[1]);
        user.uuid = UUID.randomUUID();
        server.userService.saveUser(user);
        LogHelper.info("User %s registered. UUID: %s", user.username, user.uuid.toString());
    }
}
