package pro.gravit.launchserver.command.install;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class MultiCommand extends Command {
    public MultiCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return null;
    }

    @Override
    public void invoke(String... args) {
        for (String arg : args) {
            server.commandHandler.eval(arg, false);
        }
    }
}
