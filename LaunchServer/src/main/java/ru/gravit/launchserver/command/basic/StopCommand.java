package ru.gravit.launchserver.command.basic;

import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

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
    @SuppressWarnings("CallToSystemExit")
    public void invoke(String... args) {
        JVMHelper.RUNTIME.exit(0);
    }
}
