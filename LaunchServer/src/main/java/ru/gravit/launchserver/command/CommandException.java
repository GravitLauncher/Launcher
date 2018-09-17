package ru.gravit.launchserver.command;

import ru.gravit.launcher.LauncherAPI;

public final class CommandException extends Exception {
    private static final long serialVersionUID = -6588814993972117772L;

    @LauncherAPI
    public CommandException(String message) {
        super(message);
    }

    @LauncherAPI
    public CommandException(Throwable exc) {
        super(exc);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
