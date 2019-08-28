package pro.gravit.launchserver.command.modules;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class LoadModuleCommand extends Command {
    public LoadModuleCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[jar]";
    }

    @Override
    public String getUsageDescription() {
        return "Module jar file";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        Path file = Paths.get(args[0]);
        server.modulesManager.loadModule(file);
    }
}
