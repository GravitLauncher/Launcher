package pro.gravit.launchserver.command.basic;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.JVMHelper;

public final class StopCommand extends Command {
    public StopCommand(LaunchServer server) {
        super(server);
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
    public void invoke(String... args) {
        JVMHelper.RUNTIME.exit(0);
    }
}
