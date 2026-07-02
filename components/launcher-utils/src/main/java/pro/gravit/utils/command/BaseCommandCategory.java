package pro.gravit.utils.command;

import pro.gravit.utils.helper.VerifyHelper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BaseCommandCategory implements CommandCategory {
    private final Map<String, Command> commands = new ConcurrentHashMap<>(32);

    @Override
    public void registerCommand(String name, Command command) {
        VerifyHelper.verifyIDName(name);
        VerifyHelper.putIfAbsent(commands, name.toLowerCase(), Objects.requireNonNull(command, "command"),
                String.format("Command has been already registered: '%s'", name.toLowerCase()));
    }

    @Override
    public Command unregisterCommand(String name) {
        return commands.remove(name);
    }

    @Override
    public Command findCommand(String name) {
        return commands.get(name);
    }

    @Override
    public Map<String, Command> commandsMap() {
        return commands;
    }
}
