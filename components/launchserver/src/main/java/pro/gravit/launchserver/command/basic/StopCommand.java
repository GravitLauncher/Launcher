package pro.gravit.launchserver.command.basic;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public final class StopCommand extends Command {
    private final AutoCloseable shutdown;

    public StopCommand(LaunchServer server) {
        this(server, server::close);
    }

    public StopCommand(LaunchServer server, AutoCloseable shutdown) {
        super(server);
        this.shutdown = shutdown;
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Stop LaunchServer";
    }

    @Override
    public void invoke(String... args) throws Exception {
        shutdown.close();
    }
}
