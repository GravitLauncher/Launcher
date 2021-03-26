package pro.gravit.launchserver;

import pro.gravit.utils.command.Command;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows calling commands using the config command
 */
public interface Reconfigurable {
    /**
     * Gets a list of commands available for this object.
     *
     * @return Key - Command Name
     * Value is a command object
     */
    Map<String, Command> getCommands();

    default Map<String, Command> defaultCommandsMap() {
        return new HashMap<>();
    }
}
