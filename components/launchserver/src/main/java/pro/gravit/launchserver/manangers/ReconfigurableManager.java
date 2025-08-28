package pro.gravit.launchserver.manangers;

import pro.gravit.launchserver.Reconfigurable;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.command.basic.HelpCommand;
import pro.gravit.utils.helper.VerifyHelper;

import java.util.HashMap;
import java.util.Map;

public class ReconfigurableManager {
    private final HashMap<String, Command> RECONFIGURABLE = new HashMap<>();

    public void registerReconfigurable(String name, Reconfigurable reconfigurable) {
        VerifyHelper.putIfAbsent(RECONFIGURABLE, name.toLowerCase(), new ReconfigurableVirtualCommand(reconfigurable.getCommands()),
                "Reconfigurable has been already registered: '%s'".formatted(name));
    }

    public void unregisterReconfigurable(String name) {
        RECONFIGURABLE.remove(name.toLowerCase());
    }

    public void call(String name, String action, String[] args) throws Exception {
        Command commands = RECONFIGURABLE.get(name);
        if (commands == null) throw new CommandException("Reconfigurable %s not found".formatted(name));
        Command command = commands.childCommands.get(action);
        if (command == null) throw new CommandException("Action %s.%s not found".formatted(name, action));
        command.invoke(args);
    }

    public void printHelp(String name) throws CommandException {
        Command commands = RECONFIGURABLE.get(name);
        if (commands == null) throw new CommandException("Reconfigurable %s not found".formatted(name));
        HelpCommand.printSubCommandsHelp(name, commands);
    }

    public Map<String, Command> getCommands() {
        return RECONFIGURABLE;
    }

    private static class ReconfigurableVirtualCommand extends Command {
        public ReconfigurableVirtualCommand(Map<String, Command> childs) {
            super(childs);
        }

        @Override
        public String getArgsDescription() {
            return null;
        }

        @Override
        public String getUsageDescription() {
            return null;
        }

        @Override
        public void invoke(String... args) throws Exception {
            invokeSubcommands(args);
        }
    }
}
