package ru.gravit.launchserver.console;

import ru.gravit.utils.command.StdCommandHandler;

public class RemoteStdCommandHandler extends StdCommandHandler {
    public RemoteStdCommandHandler(boolean readCommands) {
        super(readCommands);
    }

    @Override
    public void eval(String line, boolean bell) {
        if (line.equals("exit")) System.exit(0);
    }
}
