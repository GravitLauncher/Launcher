package ru.gravit.launchserver.command.hash;

import java.io.IOException;

import ru.gravit.launcher.helper.LogHelper;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

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
