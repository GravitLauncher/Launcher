package pro.gravit.launchserver.command.modules;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class ModulesCommand extends Command {

    public ModulesCommand(LaunchServer server) {
        super(server);
        childCommands.put("list", new ModulesListCommand(server));
        childCommands.put("available", new ModuleAvailableListCommand(server));
        childCommands.put("load", new LoadModuleCommand(server));
        childCommands.put("launcher-load", new LoadLauncherModuleCommand(server));
        childCommands.put("launcher-reload", new ReloadLauncherModuleCommand(server));
    }

    @Override
    public String getArgsDescription() {
        return "[subcommand] [args...]";
    }

    @Override
    public String getUsageDescription() {
        return "add/remove and list modules";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}
