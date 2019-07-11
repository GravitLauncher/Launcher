package pro.gravit.launchserver;

import java.util.Map;

import pro.gravit.utils.command.Command;

public interface Reconfigurable {
    Map<String, Command> getCommands();
}
