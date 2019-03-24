package ru.gravit.launchserver.command;

import ru.gravit.launchserver.LaunchServer;

public abstract class Command extends ru.gravit.utils.command.Command {


    protected final LaunchServer server;


    protected Command(LaunchServer server) {
        this.server = server;
    }
}
