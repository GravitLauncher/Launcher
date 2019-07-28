package pro.gravit.launchserver;

import pro.gravit.utils.command.Command;

import java.util.Map;

public interface Reconfigurable {
    Map<String, Command> getCommands();
}
