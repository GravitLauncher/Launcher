package ru.gravit.launchserver.command.basic;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

import java.io.IOException;
import java.nio.file.Files;

public class RemoveMappingsProguardCommand extends Command {

    public RemoveMappingsProguardCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "Removes proguard mappings (if you want to gen new mappings).";
    }

    @Override
    public void invoke(String... args) throws IOException {
        Files.deleteIfExists(server.proguardConf.mappings);
    }

}
