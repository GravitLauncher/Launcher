package pro.gravit.launchserver.command.basic;

import java.io.IOException;
import java.nio.file.Files;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

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
