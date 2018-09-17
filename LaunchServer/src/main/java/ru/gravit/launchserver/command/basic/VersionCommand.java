package ru.gravit.launchserver.command.basic;

import ru.gravit.launcher.LauncherVersion;
import ru.gravit.launcher.helper.LogHelper;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

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
        LogHelper.subInfo("LaunchServer version: %d.%d.%d (build #%d)", LauncherVersion.MAJOR, LauncherVersion.MINOR, LauncherVersion.PATCH, LauncherVersion.BUILD);
    }
}
