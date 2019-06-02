package pro.gravit.launchserver.command;

import pro.gravit.launchserver.LaunchServer;

public abstract class Command extends pro.gravit.utils.command.Command {


    protected final LaunchServer server;


    protected Command(LaunchServer server) {
        this.server = server;
    }
}
