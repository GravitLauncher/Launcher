package ru.gravit.utils.command;

import ru.gravit.utils.helper.VerifyHelper;

import java.util.UUID;

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

    public abstract String getArgsDescription(); // "<required> [optional]"


    public abstract String getUsageDescription();


    public abstract void invoke(String... args) throws Exception;


    protected final void verifyArgs(String[] args, int min) throws CommandException {
        if (args.length < min)
            throw new CommandException("Command usage: " + getArgsDescription());
    }
}
