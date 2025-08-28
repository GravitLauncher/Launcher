package pro.gravit.launchserver.command.sync;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class SyncCommand extends Command {
    public SyncCommand(LaunchServer server) {
        super(server);
        this.childCommands.put("launchermodules", new SyncLauncherModulesCommand(server));
    }

    @Override
    public String getArgsDescription() {
        return "[updates/profiles/up/binaries/launchermodules/updatescache] [args...]";
    }

    @Override
    public String getUsageDescription() {
        return "sync specified objects";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}
