package ru.gravit.launchserver.command;

import java.util.UUID;

import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launchserver.LaunchServer;

public abstract class Command {

    protected static String parseUsername(String username) throws CommandException {
        try {
            return VerifyHelper.verifyUsername(username);
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage());
        }
    }


    protected static UUID parseUUID(String s) throws CommandException {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ignored) {
            throw new CommandException(String.format("Invalid UUID: '%s'", s));
        }
    }


    protected final LaunchServer server;


    protected Command(LaunchServer server) {
        this.server = server;
    }


    public abstract String getArgsDescription(); // "<required> [optional]"


    public abstract String getUsageDescription();


    public abstract void invoke(String... args) throws Exception;


    protected final void verifyArgs(String[] args, int min) throws CommandException {
        if (args.length < min)
            throw new CommandException("Command usage: " + getArgsDescription());
    }
}
