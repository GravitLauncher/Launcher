package pro.gravit.launchserver.command.dao;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.dao.impl.UserHibernateImpl;
import pro.gravit.utils.helper.LogHelper;

import java.util.UUID;

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
        UserHibernateImpl user = new UserHibernateImpl();
        user.username = args[0];
        user.setPassword(args[1]);
        user.uuid = UUID.randomUUID();
        server.config.dao.userDAO.save(user);
        LogHelper.info("User %s registered. UUID: %s", user.username, user.uuid.toString());
    }
}
