package pro.gravit.launchserver;

import java.util.Map;

import pro.gravit.utils.command.Command;

/**
 * Allows calling commands using the config command
 */
public interface Reconfigurable {
    /**
     * Gets a list of commands available for this object.
     * @return Key - Command Name
     * Value is a command object
     */
    Map<String, Command> getCommands();
}
