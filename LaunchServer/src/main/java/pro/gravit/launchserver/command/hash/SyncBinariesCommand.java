package pro.gravit.launchserver.command.hash;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;

public final class SyncBinariesCommand extends Command {
    public SyncBinariesCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Resync launcher binaries";
    }

    @Override
    public void invoke(String... args) throws IOException {
        server.syncLauncherBinaries();
        LogHelper.subInfo("Binaries successfully resynced");
    }
}
