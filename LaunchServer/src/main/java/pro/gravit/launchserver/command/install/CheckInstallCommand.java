package pro.gravit.launchserver.command.install;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

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
    public void invoke(String... args) {
        LogHelper.info("Check install success");
        JVMHelper.RUNTIME.exit(0);
    }
}
