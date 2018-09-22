package ru.gravit.launchserver.command.modules;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;

public class ModulesCommand extends Command {
    public ModulesCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return "get all modules";
    }

    @Override
    public void invoke(String... args) {
        server.modulesManager.printModules();
    }
}
