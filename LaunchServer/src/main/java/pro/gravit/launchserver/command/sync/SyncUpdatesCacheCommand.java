package pro.gravit.launchserver.command.sync;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class SyncUpdatesCacheCommand extends Command {
    public SyncUpdatesCacheCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "sync updates cache";
    }

    @Override
    public void invoke(String... args) throws Exception {
        server.updatesManager.readUpdatesFromCache();
    }
}
