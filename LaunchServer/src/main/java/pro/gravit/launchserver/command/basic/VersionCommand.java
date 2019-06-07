package pro.gravit.launchserver.command.basic;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

public final class VersionCommand extends Command {
    public VersionCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Print LaunchServer version";
    }

    @Override
    public void invoke(String... args) {
        LogHelper.subInfo("LaunchServer version: %d.%d.%d (build #%d)", Version.MAJOR, Version.MINOR, Version.PATCH, Version.BUILD);
    }
}
