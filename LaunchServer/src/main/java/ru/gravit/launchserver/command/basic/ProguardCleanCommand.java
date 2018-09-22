package ru.gravit.launchserver.command.basic;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

public class ProguardCleanCommand extends Command {
    public ProguardCleanCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Resets proguard config";
    }

    @Override
    public void invoke(String... args) {
        server.proguardConf.prepare(true);
    }
}
