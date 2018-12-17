package ru.gravit.launchserver.command.basic;

import java.io.IOException;
import java.nio.file.Files;

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
    public void invoke(String... args) throws IOException {
        server.proguardConf.prepare(true);
        Files.deleteIfExists(server.proguardConf.mappings);
    }
}
