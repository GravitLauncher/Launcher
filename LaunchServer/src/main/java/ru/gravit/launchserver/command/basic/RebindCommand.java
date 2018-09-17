package ru.gravit.launchserver.command.basic;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

public final class RebindCommand extends Command {
    public RebindCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Rebind server socket";
    }

    @Override
    public void invoke(String... args) {
        server.rebindServerSocket();
    }
}
