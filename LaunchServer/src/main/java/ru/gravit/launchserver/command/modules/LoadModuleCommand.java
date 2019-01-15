package ru.gravit.launchserver.command.modules;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

import java.net.URI;
import java.nio.file.Paths;

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
        URI uri = Paths.get(args[0]).toUri();
        server.modulesManager.loadModuleFull(uri.toURL());
    }
}
