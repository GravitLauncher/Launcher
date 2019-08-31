package pro.gravit.launchserver.command.modules;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

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
        server.modulesManager.printModulesInfo();
    }
}
