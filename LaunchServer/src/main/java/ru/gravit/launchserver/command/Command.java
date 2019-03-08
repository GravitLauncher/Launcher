package ru.gravit.launchserver.command;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.command.CommandException;
import ru.gravit.utils.helper.VerifyHelper;

import java.util.UUID;

public abstract class Command extends ru.gravit.utils.command.Command {


    protected final LaunchServer server;


    protected Command(LaunchServer server) {
        this.server = server;
    }
}
