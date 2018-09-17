package ru.gravit.launchserver.command;

import java.util.UUID;

import ru.gravit.launcher.LauncherAPI;
import ru.gravit.launcher.helper.VerifyHelper;
import ru.gravit.launchserver.LaunchServer;

public abstract class Command {
    @LauncherAPI
    protected static String parseUsername(String username) throws CommandException {
        try {
            return VerifyHelper.verifyUsername(username);
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage());
        }
    }

    @LauncherAPI
    protected static UUID parseUUID(String s) throws CommandException {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ignored) {
            throw new CommandException(String.format("Invalid UUID: '%s'", s));
        }
    }

    @LauncherAPI
    protected final LaunchServer server;

    @LauncherAPI
    protected Command(LaunchServer server) {
        this.server = server;
    }

    @LauncherAPI
    public abstract String getArgsDescription(); // "<required> [optional]"

    @LauncherAPI
    public abstract String getUsageDescription();

    @LauncherAPI
    public abstract void invoke(String... args) throws Exception;

    @LauncherAPI
    protected final void verifyArgs(String[] args, int min) throws CommandException {
        if (args.length < min)
			throw new CommandException("Command usage: " + getArgsDescription());
    }
}
