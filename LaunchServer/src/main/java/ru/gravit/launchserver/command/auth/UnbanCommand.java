package ru.gravit.launchserver.command.auth;

import ru.gravit.launcher.HWID;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

import java.util.List;

public class UnbanCommand extends Command {
    public UnbanCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[username]";
    }

    @Override
    public String getUsageDescription() {
        return "Unban username for HWID";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        List<HWID> target = server.config.hwidHandler.getHwid(args[0]);
        server.config.hwidHandler.unban(target);
    }
}
