package ru.gravit.launchserver.command.install;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.JVMHelper;
import ru.gravit.utils.helper.LogHelper;

public class CheckInstallCommand extends Command {
    public CheckInstallCommand(LaunchServer server) {
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
    public void invoke(String... args) throws Exception {
        LogHelper.info("Check install success");
        JVMHelper.RUNTIME.exit(0);
    }
}
