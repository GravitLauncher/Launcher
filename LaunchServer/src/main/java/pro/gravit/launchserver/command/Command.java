package pro.gravit.launchserver.command;

import pro.gravit.launchserver.LaunchServer;

import java.util.Map;

public abstract class Command extends pro.gravit.utils.command.Command {
    protected final LaunchServer server;


    protected Command(LaunchServer server) {
        super();
        this.server = server;
    }

    public Command(Map<String, pro.gravit.utils.command.Command> childCommands, LaunchServer server) {
        super(childCommands);
        this.server = server;
    }
}
